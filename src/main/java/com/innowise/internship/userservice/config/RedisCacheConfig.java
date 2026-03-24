package com.innowise.internship.userservice.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration(CacheProperties cacheProperties) {
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType("com.innowise.internship")
                                .allowIfBaseType("java.util")
                                .build())
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getDefaultTtl())
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration defaultCacheConfig,
            CacheProperties cacheProperties) {
        return builder -> builder
                .withCacheConfiguration("users",
                        defaultCacheConfig.entryTtl(cacheProperties.getTtlForCache("users")))
                .withCacheConfiguration("users_pages",
                        defaultCacheConfig.entryTtl(cacheProperties.getTtlForCache("users_pages")))
                .withCacheConfiguration("cards",
                        defaultCacheConfig.entryTtl(cacheProperties.getTtlForCache("cards")))
                .withCacheConfiguration("cards_pages",
                        defaultCacheConfig.entryTtl(cacheProperties.getTtlForCache("cards_pages")))
                .withCacheConfiguration("cards_user",
                        defaultCacheConfig.entryTtl(cacheProperties.getTtlForCache("cards_user")));
    }
}
