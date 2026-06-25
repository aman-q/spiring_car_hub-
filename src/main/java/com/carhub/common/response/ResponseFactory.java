package com.carhub.common.response;

import com.carhub.common.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Builds {@link ApiResponse} envelopes with messages resolved from the bundle, so
 * controllers never embed status codes or literal message strings.
 */
@Component
@RequiredArgsConstructor
public class ResponseFactory {

    private final MessageService messageService;

    public <T> ResponseEntity<ApiResponse<T>> ok(String messageKey, T data) {
        return ResponseEntity.ok(ApiResponse.success(messageService.get(messageKey), data));
    }

    public ResponseEntity<ApiResponse<Void>> ok(String messageKey) {
        return ResponseEntity.ok(ApiResponse.success(messageService.get(messageKey)));
    }

    public <T> ResponseEntity<ApiResponse<T>> created(String messageKey, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(messageService.get(messageKey), data));
    }
}
