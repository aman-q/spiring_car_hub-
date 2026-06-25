package com.carhub.abuselog;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Persisted record of a rate-limit violation, for abuse monitoring.
 */
@Getter
@Builder
@Document(collection = "abuselogs")
public class AbuseLog {

    @Id
    private String id;
    private String ipAddress;
    private String route;
    private String method;
    private long exceededLimit;
    private long allowedLimit;
    private long windowSec;
    private Instant createdAt;
}
