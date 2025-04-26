package com.andrey.semeynikov.balanceron.controller;

import com.andrey.semeynikov.balanceron.service.HttpForwardService;
import com.andrey.semeynikov.balanceron.service.LoadBalancingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller that intercepts all incoming requests and forwards them to the appropriate service
 * instance based on the current load balancing algorithm.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ProxyController {

  private final LoadBalancingService loadBalancingService;
  private final HttpForwardService httpForwardService;

  /**
   * Handles all incoming requests and proxies them to the selected service instance.
   *
   * @param request The incoming HTTP request
   * @return Response from the selected service instance or an error response if no instances are
   *     available
   */
  @RequestMapping("/api/**")
  public Mono<ResponseEntity<byte[]>> proxyRequest(ServerHttpRequest request) {
    String path = request.getPath().value();
    String method = request.getMethod().name();

    log.info("Received request: {} {}", method, path);

    return loadBalancingService
        .chooseInstance()
        .map(
            instance -> {
              log.info(
                  "Routing request to instance: {} ({}:{})",
                  instance.getId(),
                  instance.getHost(),
                  instance.getPort());
              return httpForwardService.forwardRequest(request, instance);
            })
        .orElseGet(
            () -> {
              log.warn("No available service instances for request: {} {}", method, path);
              return Mono.just(
                  ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                      .body("No available service instances".getBytes()));
            });
  }
}
