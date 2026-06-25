package com.carhub.car;

import com.carhub.car.dto.AddCarRequest;
import com.carhub.car.dto.CarDetailResponse;
import com.carhub.car.dto.CarResponse;
import com.carhub.car.dto.PagedCarsResponse;
import com.carhub.car.dto.UpdateCarRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface CarService {

    PagedCarsResponse getCars(int page, int limit, LocalDate startDate, LocalDate endDate);

    CarResponse addCar(String userId, AddCarRequest request, MultipartFile[] images);

    CarResponse updateCar(String userId, String carId, UpdateCarRequest request, MultipartFile[] images);

    void removeCar(String userId, String carId);

    List<CarResponse> getUserCars(String userId);

    CarDetailResponse getCarDetail(String carId);
}
