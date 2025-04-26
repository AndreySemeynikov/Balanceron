package com.andrey.semeynikov.echounit.config;


import com.andrey.semeynikov.echounit.model.RegistrationRequest;
import com.andrey.semeynikov.echounit.model.RegistrationResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryClient {
  private final WebClient webClient;
  private final String registryUrl;
  private final String instanceId;

  @Value("${server.port}")
  private int serverPort;

  @Value("${server.address:localhost}")
  private String serverHost;

  @Value("${INSTANCE_NAME:}")
  String instanceNameEnv;

  @Value("${INSTANCE_WEIGHT:}")
  int weight;

  @PostConstruct
  public void init() {
    registerWithRegistry();
    scheduleHeartbeat();
  }

  private void registerWithRegistry() {
    RegistrationRequest request =
        new RegistrationRequest(instanceId, instanceNameEnv, serverHost, serverPort, weight);

    webClient
        .post()
        .uri(registryUrl + "/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(RegistrationResponse.class)
        .doOnSuccess(response -> log.info("Successfully registered with registry: {}", response))
        .doOnError(error -> log.error("Failed to register with registry", error))
        .subscribe();
  }

  private void scheduleHeartbeat() {
    Flux.interval(Duration.ofSeconds(15)).subscribe(tick -> sendHeartbeat());
  }

  private void sendHeartbeat() {
    webClient
        .put()
        .uri(registryUrl + "/heartbeat/{instanceId}", instanceId)
        .retrieve()
        .bodyToMono(String.class)
        .doOnSuccess(response -> log.debug("Heartbeat sent successfully"))
        .doOnError(error -> log.error("Failed to send heartbeat", error))
        .subscribe();
  }

  @PreDestroy
  public void deregister() {
    webClient
        .delete()
        .uri(registryUrl + "/dashboard/{instanceId}", instanceId)
        .retrieve()
        .bodyToMono(String.class)
        .doOnSuccess(response -> log.info("Successfully deregistered from registry"))
        .doOnError(error -> log.error("Failed to deregister from registry", error))
        .subscribe();
  }
}
