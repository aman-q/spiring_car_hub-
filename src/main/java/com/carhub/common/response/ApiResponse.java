package com.carhub.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard response envelope returned by every endpoint.
 *
 * <p>Shape: {@code { "success": boolean, "message": string, "data": T? }}. The
 * {@code data} field is omitted from the JSON when {@code null} so success/error
 * payloads stay clean.
 *
 * @param <T> type of the payload carried in {@code data}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
