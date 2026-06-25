package com.carhub.common.security;

import com.carhub.common.exception.ErrorCode;
import com.carhub.common.message.MessageService;
import com.carhub.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns the {@link ApiResponse} error envelope (401) when an unauthenticated
 * request hits a protected route, instead of Spring Security's default HTML page.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final MessageService messages;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(ErrorCode.UNAUTHENTICATED.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String message = messages.get(ErrorCode.UNAUTHENTICATED.getMessageKey());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
