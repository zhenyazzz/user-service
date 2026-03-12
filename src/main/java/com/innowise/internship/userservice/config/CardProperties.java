package com.innowise.internship.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.cards")
@Getter
@Setter
public class CardProperties {

    private int maxPerUser = 5;
}
