package com.carhub.car;

import com.carhub.booking.BookingRepository;
import com.carhub.common.exception.ApiException;
import com.carhub.common.exception.ErrorCode;
import com.carhub.user.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {

    @Mock
    CarRepository carRepository;
    @Mock
    BookingRepository bookingRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    CarMapper carMapper;
    @Mock
    CloudinaryService cloudinaryService;
    @InjectMocks
    CarServiceImpl service;

    @Test
    void removeCar_isBlockedWhenActiveBookingsExist() {
        String userId = "owner-1";
        String carId = new ObjectId().toHexString();
        Car car = Car.builder().id(carId).addedBy(userId).images(List.of("u1")).build();
        when(carRepository.findById(carId)).thenReturn(Optional.of(car));
        when(bookingRepository.existsByCarAndDeletedFalseAndStatusIn(eq(carId), anyList())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.removeCar(userId, carId));

        assertEquals(ErrorCode.CAR_HAS_ACTIVE_BOOKINGS, ex.getErrorCode());
        verify(carRepository, never()).delete(any());
    }

    @Test
    void removeCar_deletesCarAndImagesWhenNoActiveBookings() {
        String userId = "owner-1";
        String carId = new ObjectId().toHexString();
        Car car = Car.builder().id(carId).addedBy(userId).images(List.of("u1", "u2")).build();
        when(carRepository.findById(carId)).thenReturn(Optional.of(car));
        when(bookingRepository.existsByCarAndDeletedFalseAndStatusIn(eq(carId), anyList())).thenReturn(false);

        service.removeCar(userId, carId);

        verify(carRepository).delete(car);
        verify(cloudinaryService).deleteByUrls(car.getImages());
    }
}
