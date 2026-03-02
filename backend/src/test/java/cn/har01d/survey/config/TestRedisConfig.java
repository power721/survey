package cn.har01d.survey.config;

import cn.har01d.survey.service.RateLimitService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory = Mockito.mock(RedisConnectionFactory.class);
        RedisConnection connection = Mockito.mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        return factory;
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);

        when(template.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(template.opsForValue()).thenReturn(valueOps);
        when(template.hasKey(anyString())).thenReturn(false);
        when(template.keys(anyString())).thenReturn(java.util.Collections.emptySet());

        doAnswer(inv -> null).when(valueOps).set(anyString(), anyString());
        doAnswer(inv -> null).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        return template;
    }

    @Bean
    @Primary
    public RateLimitService rateLimitService(StringRedisTemplate stringRedisTemplate) {
        RateLimitService service = Mockito.mock(RateLimitService.class);
        when(service.isRateLimited(anyString())).thenReturn(false);
        when(service.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(service.hasVoted(anyString(), anyString())).thenReturn(false);
        when(service.hasVotedDaily(anyString(), anyString())).thenReturn(false);
        return service;
    }
}
