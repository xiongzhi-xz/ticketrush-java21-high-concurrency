package com.ticketrush.infrastructure.redis;

import com.ticketrush.application.service.RushAdmissionGuard;
import com.ticketrush.common.api.ErrorCode;
import com.ticketrush.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisRushAdmissionGuardTest {

    @SuppressWarnings("unchecked")
    @Test
    void shouldAcquireAndReleaseAdmissionToken() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisScript<Long> acquireScript = mock(RedisScript.class);
        RedisScript<Long> releaseScript = mock(RedisScript.class);
        RedisRushAdmissionGuard guard = new RedisRushAdmissionGuard(
                redisTemplate,
                new InventoryRedisKeyFactory("ticketrush"),
                acquireScript,
                releaseScript,
                true,
                500,
                Duration.ofSeconds(10)
        );

        when(redisTemplate.execute(
                eq(acquireScript),
                eq(List.of("ticketrush:rush:admission:1001")),
                eq("500"),
                eq("10")
        )).thenReturn(1L);

        try (RushAdmissionGuard.Permit ignored = guard.acquire(1001L)) {
            assertThat(ignored).isNotNull();
        }

        verify(redisTemplate).execute(
                eq(acquireScript),
                eq(List.of("ticketrush:rush:admission:1001")),
                eq("500"),
                eq("10")
        );
        verify(redisTemplate).execute(eq(releaseScript), eq(List.of("ticketrush:rush:admission:1001")));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldRejectWhenAdmissionTokenUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisScript<Long> acquireScript = mock(RedisScript.class);
        RedisScript<Long> releaseScript = mock(RedisScript.class);
        RedisRushAdmissionGuard guard = new RedisRushAdmissionGuard(
                redisTemplate,
                new InventoryRedisKeyFactory("ticketrush"),
                acquireScript,
                releaseScript,
                true,
                500,
                Duration.ofSeconds(10)
        );

        when(redisTemplate.execute(
                eq(acquireScript),
                eq(List.of("ticketrush:rush:admission:1001")),
                eq("500"),
                eq("10")
        )).thenReturn(0L);

        assertThatThrownBy(() -> guard.acquire(1001L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.errorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
                });
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldBypassRedisWhenAdmissionDisabled() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisScript<Long> acquireScript = mock(RedisScript.class);
        RedisScript<Long> releaseScript = mock(RedisScript.class);
        RedisRushAdmissionGuard guard = new RedisRushAdmissionGuard(
                redisTemplate,
                new InventoryRedisKeyFactory("ticketrush"),
                acquireScript,
                releaseScript,
                false,
                500,
                Duration.ofSeconds(10)
        );

        try (RushAdmissionGuard.Permit ignored = guard.acquire(1001L)) {
            assertThat(ignored).isNotNull();
        }

        verifyNoInteractions(redisTemplate);
    }
}
