package cn.har01d.survey.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean isAllowed(String key, int maxRequests, long windowMillis) {
        Bucket bucket = buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.simple(maxRequests, Duration.ofMillis(windowMillis)))
                        .build()
        );
        return bucket.tryConsume(1);
    }
}
