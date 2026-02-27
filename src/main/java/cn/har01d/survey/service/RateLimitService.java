package cn.har01d.survey.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

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

    public boolean hasVoted(String pollId, String identifier) {
        String key = "vote:" + pollId + ":" + identifier;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void markVoted(String pollId, String identifier) {
        String key = "vote:" + pollId + ":" + identifier;
        redisTemplate.opsForValue().set(key, "1");
    }

    public void markVotedDaily(String pollId, String identifier) {
        String key = "vote:daily:" + pollId + ":" + identifier + ":" + LocalDate.now();
        redisTemplate.opsForValue().set(key, "1", Duration.ofDays(1));
    }

    public boolean hasVotedDaily(String pollId, String identifier) {
        String key = "vote:daily:" + pollId + ":" + identifier + ":" + LocalDate.now();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
