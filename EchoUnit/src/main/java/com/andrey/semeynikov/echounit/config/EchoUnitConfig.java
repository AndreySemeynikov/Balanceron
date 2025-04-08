package com.andrey.semeynikov.echounit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "echounit")
public class EchoUnitConfig {
    private String registryUrl;
}