package com.carhub.booking;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends MongoRepository<Booking, String> {

    Optional<Booking> findByIdAndDeletedFalse(String id);

    boolean existsByCarAndDeletedFalseAndStatusIn(String car, Collection<BookingStatus> statuses);

    List<Booking> findByUserAndDeletedFalseOrderByCreatedAtDesc(String user);

    List<Booking> findByUserAndStatusAndDeletedFalseOrderByCreatedAtDesc(String user, BookingStatus status);

    List<Booking> findByCarAndDeletedFalseAndStatusInAndEndDateGreaterThanEqualOrderByStartDateAsc(
            String car, Collection<BookingStatus> statuses, LocalDate today);

    List<Booking> findByUserAndDeletedFalseAndStatusInAndEndDateGreaterThanEqualOrderByStartDateAsc(
            String user, Collection<BookingStatus> statuses, LocalDate today);

    /** True if an active (pending/confirmed) booking overlaps [start, end] for the car. */
    @Query(value = "{ 'car': ?0, 'deleted': false, 'status': { $in: ?1 }, "
            + "'startDate': { $lte: ?2 }, 'endDate': { $gte: ?3 } }", exists = true)
    boolean existsActiveOverlap(String carId, Collection<BookingStatus> statuses, LocalDate end, LocalDate start);

    /** Active bookings overlapping [start, end] across all cars (car field only). */
    @Query(value = "{ 'deleted': false, 'status': { $in: ?0 }, "
            + "'startDate': { $lte: ?1 }, 'endDate': { $gte: ?2 } }", fields = "{ 'car': 1 }")
    List<Booking> findActiveOverlapping(Collection<BookingStatus> statuses, LocalDate end, LocalDate start);

    /** Active bookings covering the given day across all cars (car field only). */
    @Query(value = "{ 'deleted': false, 'status': { $in: ?0 }, "
            + "'startDate': { $lte: ?1 }, 'endDate': { $gte: ?1 } }", fields = "{ 'car': 1 }")
    List<Booking> findActiveOnDate(Collection<BookingStatus> statuses, LocalDate date);

    /** Active bookings covering the given day, restricted to a set of cars (car field only). */
    @Query(value = "{ 'car': { $in: ?0 }, 'deleted': false, 'status': { $in: ?1 }, "
            + "'startDate': { $lte: ?2 }, 'endDate': { $gte: ?2 } }", fields = "{ 'car': 1 }")
    List<Booking> findActiveOnDateForCars(Collection<String> carIds, Collection<BookingStatus> statuses, LocalDate date);

    /** Completed/cancelled bookings, plus expired pending/confirmed ones. */
    @Query(value = "{ 'user': ?0, 'deleted': false, $or: [ { 'status': { $in: ?1 } }, "
            + "{ 'status': { $in: ?2 }, 'endDate': { $lt: ?3 } } ] }", sort = "{ 'startDate': -1 }")
    List<Booking> findPastBookings(String user, Collection<BookingStatus> finishedStatuses,
                                   Collection<BookingStatus> activeStatuses, LocalDate today);
}
