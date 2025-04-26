package com.andrey.semeynikov.echounit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Configuration
public class WebClientConfig {

  @Value("${echounit.registry-url:}")
  private String registryUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl(registryUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Bean
    public String registryUrl() {
        return registryUrl;
    }

    @Bean
    public String instanceId() {
        return UUID.randomUUID().toString();
    }
}
