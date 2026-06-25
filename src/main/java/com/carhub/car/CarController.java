package com.carhub.car;

import com.carhub.car.dto.AddCarRequest;
import com.carhub.car.dto.CarDetailResponse;
import com.carhub.car.dto.CarResponse;
import com.carhub.car.dto.PagedCarsResponse;
import com.carhub.car.dto.UpdateCarRequest;
import com.carhub.common.message.MessageKeys;
import com.carhub.common.response.ApiResponse;
import com.carhub.common.response.ResponseFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Cars", description = "Car listings and availability")
@RestController
@RequestMapping("/api/v1/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final ResponseFactory response;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedCarsResponse>> getAllCars(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return response.ok(MessageKeys.CARS_FETCHED, carService.getCars(page, limit, startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CarResponse>> addCar(
            @AuthenticationPrincipal String userId,
            @Valid @ModelAttribute AddCarRequest request,
            @RequestParam("images") MultipartFile[] images) {
        return response.created(MessageKeys.CAR_ADDED, carService.addCar(userId, request, images));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CarResponse>> updateCar(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @Valid @ModelAttribute UpdateCarRequest request,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        return response.ok(MessageKeys.CAR_UPDATED, carService.updateCar(userId, id, request, images));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeCar(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        carService.removeCar(userId, id);
        return response.ok(MessageKeys.CAR_DELETED);
    }

    @GetMapping("/usercars")
    public ResponseEntity<ApiResponse<List<CarResponse>>> getUserCars(@AuthenticationPrincipal String userId) {
        return response.ok(MessageKeys.USER_CARS_FETCHED, carService.getUserCars(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarDetailResponse>> getCarDetail(@PathVariable String id) {
        return response.ok(MessageKeys.CAR_DETAIL_FETCHED, carService.getCarDetail(id));
    }
}
