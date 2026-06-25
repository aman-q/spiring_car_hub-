package com.carhub.car;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface CarRepository extends MongoRepository<Car, String> {

    List<Car> findByAddedBy(String addedBy);

    Page<Car> findByIdNotIn(Collection<String> ids, Pageable pageable);
}
