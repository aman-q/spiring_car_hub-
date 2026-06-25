package com.carhub.car;

import com.carhub.booking.Booking;
import com.carhub.booking.BookingRepository;
import com.carhub.booking.BookingStatus;
import com.carhub.car.dto.AddCarRequest;
import com.carhub.car.dto.AvailabilityResponse;
import com.carhub.car.dto.BookedPeriod;
import com.carhub.car.dto.CarDetailResponse;
import com.carhub.car.dto.CarResponse;
import com.carhub.car.dto.CarSummaryResponse;
import com.carhub.car.dto.OwnerInfo;
import com.carhub.car.dto.PagedCarsResponse;
import com.carhub.car.dto.UpdateCarRequest;
import com.carhub.common.exception.ApiException;
import com.carhub.common.exception.ErrorCode;
import com.carhub.user.User;
import com.carhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {

    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final int MIN_IMAGES = 3;

    private final CarRepository carRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CarMapper carMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public PagedCarsResponse getCars(int page, int limit, LocalDate startDate, LocalDate endDate) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(safePage - 1, safeLimit);
        LocalDate today = LocalDate.now();

        Page<Car> carPage;
        if (startDate != null && endDate != null && endDate.isAfter(startDate)) {
            Set<String> bookedInRange = carIds(bookingRepository.findActiveOverlapping(ACTIVE_STATUSES, endDate, startDate));
            carPage = bookedInRange.isEmpty()
                    ? carRepository.findAll(pageable)
                    : carRepository.findByIdNotIn(bookedInRange, pageable);
        } else {
            carPage = carRepository.findAll(pageable);
        }

        Set<String> bookedToday = carIds(bookingRepository.findActiveOnDate(ACTIVE_STATUSES, today));

        List<CarSummaryResponse> cars = carPage.getContent().stream()
                .map(car -> carMapper.toSummary(car, !bookedToday.contains(car.getId())))
                .toList();

        return new PagedCarsResponse(cars, carPage.getTotalPages(), safePage, carPage.getTotalElements());
    }

    @Override
    public CarResponse addCar(String userId, AddCarRequest request, MultipartFile[] images) {
        if (images == null || images.length < MIN_IMAGES) {
            throw new ApiException(ErrorCode.AT_LEAST_THREE_IMAGES);
        }
        List<String> imageUrls = cloudinaryService.uploadImages(images);
        if (imageUrls.size() < MIN_IMAGES) {
            cloudinaryService.deleteByUrls(imageUrls);
            throw new ApiException(ErrorCode.AT_LEAST_THREE_IMAGES);
        }

        Car car = Car.builder()
                .addedBy(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .images(imageUrls)
                .yearOfManufacture(request.getYearOfManufacture())
                .company(request.getCompany())
                .driveType(request.getDriveType())
                .price(request.getPrice())
                .build();

        Car saved;
        try {
            saved = carRepository.save(car);
        } catch (RuntimeException e) {
            // Roll back the just-uploaded images so they don't orphan in Cloudinary.
            cloudinaryService.deleteByUrls(imageUrls);
            throw e;
        }
        log.info("Car added: {} by {}", saved.getId(), userId);
        return carMapper.toResponse(saved);
    }

    @Override
    public CarResponse updateCar(String userId, String carId, UpdateCarRequest request, MultipartFile[] images) {
        Car car = findOwnedCar(userId, carId);

        boolean changed = false;
        if (StringUtils.hasText(request.getTitle())) {
            car.setTitle(request.getTitle());
            changed = true;
        }
        if (StringUtils.hasText(request.getDescription())) {
            car.setDescription(request.getDescription());
            changed = true;
        }
        if (StringUtils.hasText(request.getCompany())) {
            car.setCompany(request.getCompany());
            changed = true;
        }
        if (request.getDriveType() != null) {
            car.setDriveType(request.getDriveType());
            changed = true;
        }
        if (request.getYearOfManufacture() != null) {
            car.setYearOfManufacture(request.getYearOfManufacture());
            changed = true;
        }
        if (request.getPrice() != null) {
            car.setPrice(request.getPrice());
            changed = true;
        }
        List<String> supersededImages = null;
        if (images != null && images.length > 0) {
            List<String> imageUrls = cloudinaryService.uploadImages(images);
            if (!imageUrls.isEmpty()) {
                supersededImages = car.getImages();
                car.setImages(imageUrls);
                changed = true;
            }
        }

        if (!changed) {
            throw new ApiException(ErrorCode.NO_FIELDS_TO_UPDATE);
        }

        Car saved = carRepository.save(car);
        // Only purge the old images once the new set is safely persisted.
        cloudinaryService.deleteByUrls(supersededImages);
        log.info("Car updated: {}", saved.getId());
        return carMapper.toResponse(saved);
    }

    @Override
    public void removeCar(String userId, String carId) {
        Car car = findOwnedCar(userId, carId);
        if (bookingRepository.existsByCarAndDeletedFalseAndStatusIn(carId, ACTIVE_STATUSES)) {
            throw new ApiException(ErrorCode.CAR_HAS_ACTIVE_BOOKINGS);
        }
        carRepository.delete(car);
        cloudinaryService.deleteByUrls(car.getImages());
        log.info("Car removed: {}", carId);
    }

    @Override
    public List<CarResponse> getUserCars(String userId) {
        List<Car> cars = carRepository.findByAddedBy(userId);
        if (cars.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        List<String> carIdList = cars.stream().map(Car::getId).toList();
        Set<String> bookedToday = carIds(
                bookingRepository.findActiveOnDateForCars(carIdList, ACTIVE_STATUSES, today));

        OwnerInfo owner = carMapper.toOwnerInfo(userRepository.findById(userId).orElse(null));

        return cars.stream()
                .map(car -> carMapper.toResponse(car, owner, bookedToday.contains(car.getId())))
                .toList();
    }

    @Override
    public CarDetailResponse getCarDetail(String carId) {
        if (!ObjectId.isValid(carId)) {
            throw new ApiException(ErrorCode.CAR_NOT_FOUND);
        }
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ApiException(ErrorCode.CAR_NOT_FOUND));

        OwnerInfo owner = carMapper.toOwnerInfo(userRepository.findById(car.getAddedBy()).orElse(null));

        LocalDate today = LocalDate.now();
        List<Booking> active = bookingRepository
                .findByCarAndDeletedFalseAndStatusInAndEndDateGreaterThanEqualOrderByStartDateAsc(
                        carId, ACTIVE_STATUSES, today);

        Booking current = active.stream()
                .filter(b -> !b.getStartDate().isAfter(today) && !b.getEndDate().isBefore(today))
                .findFirst()
                .orElse(null);
        boolean currentlyBooked = current != null;

        LocalDate nextAvailableFrom = currentlyBooked ? nextAvailableFrom(active, current.getEndDate()) : null;

        List<BookedPeriod> periods = active.stream()
                .map(b -> new BookedPeriod(b.getStartDate(), b.getEndDate(), b.getStatus()))
                .toList();

        CarResponse carResponse = carMapper.toResponse(car, owner, currentlyBooked);
        return new CarDetailResponse(carResponse, new AvailabilityResponse(currentlyBooked, nextAvailableFrom, periods));
    }

    /** Walks the consecutive booking chain from the current block to the first real gap. */
    private LocalDate nextAvailableFrom(List<Booking> activeBookings, LocalDate currentEnd) {
        LocalDate blockEnd = currentEnd;
        boolean extended = true;
        while (extended) {
            extended = false;
            for (Booking b : activeBookings) {
                LocalDate dayAfterBlock = blockEnd.plusDays(1);
                if (!b.getStartDate().isAfter(dayAfterBlock) && b.getEndDate().isAfter(blockEnd)) {
                    blockEnd = b.getEndDate();
                    extended = true;
                }
            }
        }
        return blockEnd.plusDays(1);
    }

    private Car findOwnedCar(String userId, String carId) {
        if (!ObjectId.isValid(carId)) {
            throw new ApiException(ErrorCode.INVALID_ID);
        }
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ApiException(ErrorCode.CAR_NOT_FOUND));
        if (!car.getAddedBy().equals(userId)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_CAR_ACTION);
        }
        return car;
    }

    private Set<String> carIds(List<Booking> bookings) {
        return bookings.stream().map(Booking::getCar).collect(Collectors.toSet());
    }
}
