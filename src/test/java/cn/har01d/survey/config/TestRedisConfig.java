package cn.har01d.survey.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.HashSet;
import java.util.Set;
import java.time.Duration;

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

        Set<String> keys = new HashSet<>();

        when(template.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(template.opsForValue()).thenReturn(valueOps);
        when(template.hasKey(anyString())).thenAnswer(inv -> keys.contains(inv.getArgument(0)));

        doAnswer(inv -> {
            keys.add(inv.getArgument(0));
            return null;
        }).when(valueOps).set(anyString(), anyString());

        doAnswer(inv -> {
            keys.add(inv.getArgument(0));
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        return template;
    }
}
