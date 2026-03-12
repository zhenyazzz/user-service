package com.innowise.internship.userservice.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.cache")
@Getter
@Setter
public class CacheProperties {

    private Duration defaultTtl = Duration.ofHours(1);

    private Map<String, Duration> cacheTtls = new HashMap<>();

    public Duration getTtlForCache(String cacheName) {
        return cacheTtls.getOrDefault(cacheName, defaultTtl);
    }
}
