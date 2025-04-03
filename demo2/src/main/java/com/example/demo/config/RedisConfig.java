package com.example.demo.config;

import com.example.demo.model.Plan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    //Establish connection with redis
    @Bean
    public RedisTemplate<String, Plan> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Plan> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        //String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        //JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
