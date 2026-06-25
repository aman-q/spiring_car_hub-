package com.carhub.common.exception;

import com.carhub.common.message.MessageService;
import com.carhub.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised translation of exceptions into the {@link ApiResponse} envelope, so
 * controllers stay free of try/catch blocks. Replaces the repeated
 * {@code sendError(...)} handling in every Node controller.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messages;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ErrorCode code = ex.getErrorCode();
        String message = messages.get(code.getMessageKey(), ex.getArgs());
        if (code.getStatus().is5xxServerError()) {
            log.error("API error [{}]: {}", code, message, ex);
        } else {
            log.warn("API error [{}]: {}", code, message);
        }
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(BindException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String message = messages.get(ErrorCode.VALIDATION_ERROR.getMessageKey());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ApiResponse.error(message, fieldErrors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        String message = messages.get(ErrorCode.ACCESS_DENIED.getMessageKey());
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus()).body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        String message = messages.get(ErrorCode.INTERNAL_ERROR.getMessageKey());
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus()).body(ApiResponse.error(message));
    }
}
