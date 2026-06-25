package com.carhub.config;

import com.carhub.booking.Booking;
import com.carhub.car.Car;
import com.carhub.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Codifies index creation so every environment converges to the same schema, run on
 * startup (replaces the ad-hoc migration script). Each index is ensured idempotently;
 * a failure (e.g. a unique index over pre-existing dirty data) is logged rather than
 * fatal, so a bad data state never blocks boot. Disable with
 * {@code carhub.index-init.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "carhub.index-init.enabled", havingValue = "true", matchIfMissing = true)
public class MongoIndexInitializer implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // Users: unique email + phone.
        ensure(User.class, new Index().on("email", Sort.Direction.ASC).unique().named("email_1"));
        ensure(User.class, new Index().on("phonenumber", Sort.Direction.ASC).unique().named("phonenumber_1"));

        // Cars: lookups by owner.
        ensure(Car.class, new Index().on("addedBy", Sort.Direction.ASC).named("addedBy_1"));

        // Bookings: overlap/availability queries and per-user listing.
        ensure(Booking.class, new Index()
                .on("car", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("startDate", Sort.Direction.ASC)
                .on("endDate", Sort.Direction.ASC)
                .named("car_status_dates"));
        ensure(Booking.class, new Index()
                .on("user", Sort.Direction.ASC)
                .on("deleted", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("user_deleted_status"));
    }

    private void ensure(Class<?> entity, Index index) {
        try {
            String name = mongoTemplate.indexOps(entity).ensureIndex(index);
            log.info("Ensured index {} on {}", name, entity.getSimpleName());
        } catch (DataAccessException e) {
            log.warn("Could not ensure index on {} ({}). Likely pre-existing data conflicts; "
                    + "resolve the data and retry.", entity.getSimpleName(), e.getMessage());
        }
    }
}
