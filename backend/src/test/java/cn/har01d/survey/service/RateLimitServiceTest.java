package cn.har01d.survey.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
    }

    // --- isRateLimited ---

    @Test
    void isRateLimited_firstCall_notLimited() {
        boolean result = rateLimitService.isRateLimited("test-key");
        assertFalse(result);
    }

    @Test
    void isRateLimited_withinLimit_notLimited() {
        String key = "rate-key";
        for (int i = 0; i < 9; i++) {
            assertFalse(rateLimitService.isRateLimited(key));
        }
    }

    @Test
    void isRateLimited_exceedsLimit_limited() {
        String key = "exceed-key";
        // Consume all 10 tokens
        for (int i = 0; i < 10; i++) {
            rateLimitService.isRateLimited(key);
        }
        // 11th should be limited
        assertTrue(rateLimitService.isRateLimited(key));
    }

    // --- hasVoted ---

    @Test
    void hasVoted_true() {
        when(redisTemplate.hasKey("vote:1:user:1")).thenReturn(true);

        assertTrue(rateLimitService.hasVoted("1", "user:1"));
    }

    @Test
    void hasVoted_false() {
        when(redisTemplate.hasKey("vote:1:user:1")).thenReturn(false);

        assertFalse(rateLimitService.hasVoted("1", "user:1"));
    }

    // --- markVoted ---

    @Test
    void markVoted() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        rateLimitService.markVoted("1", "user:1", null);

        verify(valueOperations).set("vote:1:user:1", "1", Duration.ofDays(7));
    }

    // --- markVotedDaily ---

    @Test
    void markVotedDaily() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        rateLimitService.markVotedDaily("1", "user:1");

        verify(valueOperations).set("vote:daily:1:user:1:" + LocalDate.now(), "1", Duration.ofDays(1));
    }

    // --- hasVotedDaily ---

    @Test
    void hasVotedDaily_true() {
        when(redisTemplate.hasKey("vote:daily:1:user:1:" + LocalDate.now())).thenReturn(true);

        assertTrue(rateLimitService.hasVotedDaily("1", "user:1"));
    }

    @Test
    void hasVotedDaily_false() {
        when(redisTemplate.hasKey("vote:daily:1:user:1:" + LocalDate.now())).thenReturn(false);

        assertFalse(rateLimitService.hasVotedDaily("1", "user:1"));
    }
}
