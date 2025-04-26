package com.andrey.semeynikov.balanceron.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {

  // Connection timeout in milliseconds
  private static final int CONNECT_TIMEOUT_MS = 5000;

  // Read/write timeout in seconds
  private static final int IO_TIMEOUT_SECONDS = 60;

  // Maximum size for in-memory buffering
  private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024; // 16MB

  /**
   * Creates a WebClient.Builder with appropriate configuration for proxying requests.
   *
   * @return Configured WebClient.Builder
   */
  @Bean
  public WebClient.Builder webClientBuilder() {
    // Configure connection provider with pooling
    ConnectionProvider provider =
        ConnectionProvider.builder("balanceron-connection-pool")
            .maxConnections(500)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .evictInBackground(Duration.ofSeconds(120))
            .build();

    // Configure HTTP client with timeouts
    HttpClient httpClient =
        HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(
                            new ReadTimeoutHandler(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        .addHandlerLast(
                            new WriteTimeoutHandler(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)));

    // Configure exchange strategies with larger buffer size
    ExchangeStrategies exchangeStrategies =
        ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
            .build();

    // Build and return WebClient.Builder
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(exchangeStrategies);
  }
}
    // return builder.baseUrl("http://localhost:8080").build();
