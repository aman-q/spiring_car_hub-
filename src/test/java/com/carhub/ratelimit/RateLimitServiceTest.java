package com.carhub.ratelimit;

import com.carhub.abuselog.AbuseLogRepository;
import com.carhub.common.exception.ApiException;
import com.carhub.common.exception.ErrorCode;
import com.carhub.config.properties.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> valueOps;
    @Mock
    AbuseLogRepository abuseLogRepository;
    @InjectMocks
    RateLimitService service;

    private final RateLimitProperties.Policy policy = new RateLimitProperties.Policy("t:", 2, 60);

    @Test
    void failsOpenWhenRedisIsDown() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenThrow(new DataAccessResourceFailureException("redis down"));

        assertDoesNotThrow(() -> service.enforce(new MockHttpServletRequest(), new MockHttpServletResponse(), policy));
        verify(abuseLogRepository, never()).save(any());
    }

    @Test
    void allowsRequestUnderLimit() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redis.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(60L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertDoesNotThrow(() -> service.enforce(new MockHttpServletRequest(), response, policy));
        assertEquals("2", response.getHeader("X-RateLimit-Limit"));
        assertEquals("1", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void blocksAndLogsWhenOverLimit() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(3L);
        when(redis.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(30L);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.enforce(new MockHttpServletRequest(), new MockHttpServletResponse(), policy));
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(abuseLogRepository).save(any());
    }
}
