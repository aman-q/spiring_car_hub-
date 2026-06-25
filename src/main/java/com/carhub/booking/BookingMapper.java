package com.carhub.booking;

import com.carhub.booking.dto.BookingCarInfo;
import com.carhub.booking.dto.BookingResponse;
import com.carhub.booking.dto.BookingUserInfo;
import com.carhub.car.Car;
import com.carhub.user.User;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return toResponse(booking, null, null);
    }

    public BookingResponse toResponse(Booking booking, BookingCarInfo car, BookingUserInfo user) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser(),
                booking.getCar(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getPickupLocation(),
                booking.getDropoffLocation(),
                booking.getStatus(),
                booking.getPaymentStatus(),
                booking.getPrice(),
                booking.getExtras(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                car,
                user);
    }

    public BookingCarInfo toCarInfo(Car car) {
        if (car == null) {
            return null;
        }
        return new BookingCarInfo(car.getId(), car.getTitle(), car.getCompany(),
                car.getPrice(), car.getImages(), car.getAddedBy());
    }

    public BookingUserInfo toUserInfo(User user) {
        if (user == null) {
            return null;
        }
        return new BookingUserInfo(user.getId(), user.getFname(), user.getLname(), user.getEmail());
    }
}
