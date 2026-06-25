package com.carhub.common.exception;

import lombok.Getter;

/**
 * Carries an {@link ErrorCode} (status + message key) up to the
 * {@link GlobalExceptionHandler}. Optional {@code args} feed parameterised
 * messages (e.g. the retry-after seconds for a rate-limit message).
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;

    public ApiException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = args;
    }
}
