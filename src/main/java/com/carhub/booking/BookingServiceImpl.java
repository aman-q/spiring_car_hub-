package com.carhub.booking;

import com.carhub.booking.dto.BookingResponse;
import com.carhub.booking.dto.CreateBookingRequest;
import com.carhub.booking.dto.ProfileBookingsResponse;
import com.carhub.car.Car;
import com.carhub.car.CarRepository;
import com.carhub.common.exception.ApiException;
import com.carhub.common.exception.ErrorCode;
import com.carhub.common.message.MessageKeys;
import com.carhub.common.message.MessageService;
import com.carhub.email.BookingEmailModel;
import com.carhub.email.EmailService;
import com.carhub.user.User;
import com.carhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final List<BookingStatus> FINISHED_STATUSES =
            List.of(BookingStatus.CANCELLED, BookingStatus.COMPLETED);
    private static final Set<String> ALLOWED_EXTRAS = Set.of("gps", "babySeat");
    private static final Duration BOOKING_LOCK_TTL = Duration.ofSeconds(10);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    /** Atomic compare-and-delete: only releases the lock if we still own it. */
    private static final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final EmailService emailService;
    private final MessageService messageService;
    private final StringRedisTemplate redis;

    @Override
    public BookingResponse createBooking(String userId, CreateBookingRequest request) {
        String carId = request.carId();
        String lockKey = "lock:booking:" + carId;
        String lockValue = userId + "-" + System.currentTimeMillis();

        // Per-car distributed lock to prevent double-booking races.
        Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, lockValue, BOOKING_LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new ApiException(ErrorCode.BOOKING_IN_PROGRESS);
        }

        try {
            Car car = carRepository.findById(carId)
                    .orElseThrow(() -> new ApiException(ErrorCode.CAR_NOT_FOUND));

            LocalDate start = request.startDate();
            LocalDate end = request.endDate();
            if (!end.isAfter(start)) {
                throw new ApiException(ErrorCode.INVALID_BOOKING_DATES);
            }
            if (bookingRepository.existsActiveOverlap(carId, ACTIVE_STATUSES, end, start)) {
                throw new ApiException(ErrorCode.CAR_ALREADY_BOOKED);
            }
            if (car.getPrice() == null) {
                throw new ApiException(ErrorCode.CAR_NO_PRICING);
            }

            long durationDays = ChronoUnit.DAYS.between(start, end);
            double totalPrice = durationDays * car.getPrice() + extrasCost(request.extras());

            Booking booking = Booking.builder()
                    .user(userId)
                    .car(carId)
                    .startDate(start)
                    .endDate(end)
                    .pickupLocation(request.pickupLocation())
                    .dropoffLocation(request.dropoffLocation())
                    .price(totalPrice)
                    .extras(request.extras() == null ? Map.of() : request.extras())
                    .status(BookingStatus.PENDING)
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();

            Booking saved = bookingRepository.save(booking);
            log.info("Booking created: {}", saved.getId());

            userRepository.findById(userId).ifPresent(user -> emailService.sendBookingConfirmation(
                    buildEmailModel(user, car, saved, durationDays),
                    messageService.get(MessageKeys.EMAIL_SUBJECT_BOOKING_CONFIRMATION)));

            return bookingMapper.toResponse(saved, bookingMapper.toCarInfo(car), null);
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    @Override
    public BookingResponse cancelBooking(String userId, String bookingId) {
        Booking booking = loadActiveBooking(bookingId);
        if (!booking.getUser().equals(userId)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_BOOKING_ACTION);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ApiException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ApiException(ErrorCode.BOOKING_ALREADY_COMPLETED);
        }
        if (!booking.getStartDate().isAfter(LocalDate.now())) {
            throw new ApiException(ErrorCode.CANNOT_CANCEL_STARTED_BOOKING);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking cancelled: {}", saved.getId());

        Car car = carRepository.findById(saved.getCar()).orElse(null);
        userRepository.findById(userId).ifPresent(user -> emailService.sendBookingCancellation(
                buildEmailModel(user, car, saved, days(saved)),
                messageService.get(MessageKeys.EMAIL_SUBJECT_BOOKING_CANCELLATION)));

        return bookingMapper.toResponse(saved, bookingMapper.toCarInfo(car), null);
    }

    @Override
    public BookingResponse completeBooking(String userId, String bookingId) {
        Booking booking = loadActiveBooking(bookingId);
        Car car = carRepository.findById(booking.getCar())
                .orElseThrow(() -> new ApiException(ErrorCode.CAR_NOT_FOUND));

        boolean isBookingOwner = booking.getUser().equals(userId);
        boolean isCarOwner = car.getAddedBy().equals(userId);
        if (!isBookingOwner && !isCarOwner) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_BOOKING_ACTION);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ApiException(ErrorCode.BOOKING_MUST_BE_CONFIRMED);
        }
        if (booking.getStartDate().isAfter(LocalDate.now())) {
            throw new ApiException(ErrorCode.CANNOT_COMPLETE_FUTURE_BOOKING);
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking completed: {}", saved.getId());

        userRepository.findById(saved.getUser()).ifPresent(user -> emailService.sendBookingCompletion(
                buildEmailModel(user, car, saved, days(saved)),
                messageService.get(MessageKeys.EMAIL_SUBJECT_BOOKING_COMPLETION)));

        return bookingMapper.toResponse(saved, bookingMapper.toCarInfo(car), null);
    }

    @Override
    public BookingResponse confirmBooking(String userId, String bookingId) {
        Booking booking = loadActiveBooking(bookingId);
        Car car = carRepository.findById(booking.getCar())
                .orElseThrow(() -> new ApiException(ErrorCode.CAR_NOT_FOUND));

        if (!car.getAddedBy().equals(userId)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_BOOKING_ACTION);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(ErrorCode.BOOKING_MUST_BE_PENDING);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking confirmed: {}", saved.getId());

        return bookingMapper.toResponse(saved, bookingMapper.toCarInfo(car), null);
    }

    @Override
    public List<BookingResponse> getUserBookings(String userId, BookingStatus status) {
        List<Booking> bookings = (status == null)
                ? bookingRepository.findByUserAndDeletedFalseOrderByCreatedAtDesc(userId)
                : bookingRepository.findByUserAndStatusAndDeletedFalseOrderByCreatedAtDesc(userId, status);
        return mapWithCars(bookings);
    }

    @Override
    public BookingResponse getBookingById(String userId, String bookingId) {
        Booking booking = loadActiveBooking(bookingId);
        Car car = carRepository.findById(booking.getCar()).orElse(null);
        User bookingUser = userRepository.findById(booking.getUser()).orElse(null);

        boolean isBookingOwner = booking.getUser().equals(userId);
        boolean isCarOwner = car != null && car.getAddedBy().equals(userId);
        if (!isBookingOwner && !isCarOwner) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_BOOKING_ACTION);
        }

        return bookingMapper.toResponse(booking, bookingMapper.toCarInfo(car), bookingMapper.toUserInfo(bookingUser));
    }

    @Override
    public ProfileBookingsResponse getProfileBookings(String userId) {
        LocalDate today = LocalDate.now();
        List<Booking> upcoming = bookingRepository
                .findByUserAndDeletedFalseAndStatusInAndEndDateGreaterThanEqualOrderByStartDateAsc(
                        userId, ACTIVE_STATUSES, today);
        List<Booking> past = bookingRepository.findPastBookings(userId, FINISHED_STATUSES, ACTIVE_STATUSES, today);
        return new ProfileBookingsResponse(mapWithCars(upcoming), mapWithCars(past));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Booking loadActiveBooking(String bookingId) {
        if (!ObjectId.isValid(bookingId)) {
            throw new ApiException(ErrorCode.BOOKING_NOT_FOUND);
        }
        return bookingRepository.findByIdAndDeletedFalse(bookingId)
                .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private double extrasCost(Map<String, Object> extras) {
        if (extras == null || extras.isEmpty()) {
            return 0;
        }
        for (String key : extras.keySet()) {
            if (!ALLOWED_EXTRAS.contains(key)) {
                throw new ApiException(ErrorCode.INVALID_EXTRA_OPTION);
            }
        }
        double cost = 0;
        if (isTruthy(extras.get("gps"))) {
            cost += 10;
        }
        if (isTruthy(extras.get("babySeat"))) {
            cost += 5;
        }
        return cost;
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value instanceof String s && Boolean.parseBoolean(s);
    }

    private List<BookingResponse> mapWithCars(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        Set<String> carIds = bookings.stream().map(Booking::getCar).collect(Collectors.toSet());
        Map<String, Car> carsById = carRepository.findAllById(carIds).stream()
                .collect(Collectors.toMap(Car::getId, Function.identity()));
        return bookings.stream()
                .map(b -> bookingMapper.toResponse(b, bookingMapper.toCarInfo(carsById.get(b.getCar())), null))
                .toList();
    }

    private long days(Booking booking) {
        return ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
    }

    private BookingEmailModel buildEmailModel(User user, Car car, Booking booking, long days) {
        return new BookingEmailModel(
                user.getFname(),
                user.getEmail(),
                car == null ? "your car" : car.getTitle(),
                car == null ? "" : car.getCompany(),
                bookingRef(booking.getId()),
                booking.getStartDate().format(DATE_FORMAT),
                booking.getEndDate().format(DATE_FORMAT),
                booking.getPickupLocation() == null ? "—" : booking.getPickupLocation(),
                booking.getDropoffLocation() == null ? "—" : booking.getDropoffLocation(),
                booking.getPrice(),
                days,
                booking.getStatus().name());
    }

    private String bookingRef(String id) {
        String suffix = id.length() > 8 ? id.substring(id.length() - 8) : id;
        return "#" + suffix.toUpperCase(Locale.ROOT);
    }

    private void releaseLock(String lockKey, String lockValue) {
        try {
            redis.execute(UNLOCK_SCRIPT, List.of(lockKey), lockValue);
        } catch (DataAccessException e) {
            // The lock's TTL guarantees it is released even if this best-effort delete fails.
            log.warn("Failed to release booking lock {}: {}", lockKey, e.getMessage());
        }
    }
}
