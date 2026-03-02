package cn.har01d.survey.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final Duration FALLBACK_TTL = Duration.ofDays(7);
    private static final Duration MIN_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isRateLimited(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
                        .build()
        );
        return !bucket.tryConsume(1);
    }

    public boolean isAllowed(String key, int maxRequests, long windowMillis) {
        Bucket bucket = buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.simple(maxRequests, Duration.ofMillis(windowMillis)))
                        .build()
        );
        return bucket.tryConsume(1);
    }

    public boolean hasVoted(String pollId, String identifier) {
        String key = "vote:" + pollId + ":" + identifier;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void markVoted(String pollId, String identifier, Instant endTime) {
        String key = "vote:" + pollId + ":" + identifier;
        Duration ttl = computeTtl(endTime);
        redisTemplate.opsForValue().set(key, "1", ttl);
    }

    private Duration computeTtl(Instant endTime) {
        if (endTime == null) {
            return FALLBACK_TTL;
        }
        Duration remaining = Duration.between(Instant.now(), endTime).plus(MIN_TTL);
        return remaining.isNegative() ? MIN_TTL : remaining;
    }

    public void markVotedDaily(String pollId, String identifier) {
        String key = "vote:daily:" + pollId + ":" + identifier + ":" + LocalDate.now();
        redisTemplate.opsForValue().set(key, "1", Duration.ofDays(1));
    }

    public boolean hasVotedDaily(String pollId, String identifier) {
        String key = "vote:daily:" + pollId + ":" + identifier + ":" + LocalDate.now();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredRedisKeys() {
        Set<String> keys = redisTemplate.keys("vote:*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        int cleaned = 0;
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key);
            if (ttl != null && ttl == -1) {
                redisTemplate.expire(key, FALLBACK_TTL);
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.info("Set TTL on {} Redis vote key(s) that had no expiration", cleaned);
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupRateLimitBuckets() {
        int sizeBefore = buckets.size();
        if (sizeBefore > 1000) {
            buckets.clear();
            log.info("Cleared {} rate limit buckets", sizeBefore);
        }
    }
}
