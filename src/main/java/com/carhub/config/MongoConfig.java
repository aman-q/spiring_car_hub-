package com.carhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables {@code @CreatedDate}/{@code @LastModifiedDate} auditing so documents get
 * {@code createdAt}/{@code updatedAt} automatically — the equivalent of Mongoose's
 * {@code { timestamps: true }}.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
