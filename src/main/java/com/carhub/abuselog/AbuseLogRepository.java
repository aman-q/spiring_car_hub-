package com.carhub.abuselog;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AbuseLogRepository extends MongoRepository<AbuseLog, String> {
}
