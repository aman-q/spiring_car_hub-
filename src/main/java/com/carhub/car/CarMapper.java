package com.carhub.car;

import com.carhub.car.dto.CarResponse;
import com.carhub.car.dto.CarSummaryResponse;
import com.carhub.car.dto.OwnerInfo;
import com.carhub.user.User;
import org.springframework.stereotype.Component;

/**
 * Hand-written mapper: car responses carry context-dependent fields (owner,
 * availability flags) that the service computes, so explicit mapping is clearer
 * here than annotation-based generation.
 */
@Component
public class CarMapper {

    public CarResponse toResponse(Car car) {
        return toResponse(car, null, null);
    }

    public CarResponse toResponse(Car car, OwnerInfo owner, Boolean currentlyBooked) {
        return new CarResponse(
                car.getId(),
                car.getAddedBy(),
                car.getTitle(),
                car.getDescription(),
                car.getImages(),
                car.getTags(),
                car.getYearOfManufacture(),
                car.getCompany(),
                car.getDriveType(),
                car.getPrice(),
                car.getCreatedAt(),
                car.getUpdatedAt(),
                owner,
                currentlyBooked);
    }

    public CarSummaryResponse toSummary(Car car, boolean currentlyAvailable) {
        return new CarSummaryResponse(
                car.getId(),
                car.getTitle(),
                car.getDescription(),
                car.getPrice(),
                car.getImages(),
                car.getCompany(),
                car.getYearOfManufacture(),
                car.getDriveType(),
                currentlyAvailable);
    }

    public OwnerInfo toOwnerInfo(User user) {
        if (user == null) {
            return null;
        }
        return new OwnerInfo(user.getId(), user.getFname(), user.getLname(), user.getEmail());
    }
}
