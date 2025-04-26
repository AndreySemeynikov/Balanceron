package com.andrey.semeynikov.echounit.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Controller that handles various types of requests for load balancing testing */
@RestController
@Slf4j
@RequestMapping("/api")
public class EchoController {

  @Value("${server.port}")
  private int serverPort;

  @Value("${INSTANCE_NAME:}")
  private String instanceName;

  private final Random random = new Random();
  private final AtomicInteger requestCounter = new AtomicInteger(0);

  // Simulation of active connections
  private final Map<String, LocalDateTime> activeSessions = new ConcurrentHashMap<>();

  /** Quick request - simply returns a response */
  @GetMapping("/echo")
  public ResponseEntity<Map<String, Object>> echo() {
    int count = requestCounter.incrementAndGet();
    log.info("Received echo request #{}", count);

    return ResponseEntity.ok(
        Map.of(
            "message", "Hello from EchoUnit!",
            "instanceId", instanceName,
            "port", serverPort,
            "timestamp", LocalDateTime.now().toString(),
            "requestCount", count));
  }

  /**
   * Delayed request - simulates long processing time
   *
   * @param delayMs delay in milliseconds (default 2000)
   */
  @GetMapping("/echo/delay")
  public Mono<ResponseEntity<Map<String, Object>>> delayedEcho(
      @RequestParam(defaultValue = "2000") int delayMs) {
    int count = requestCounter.incrementAndGet();
    log.info("Received delayed echo request #{} with delay {}ms", count, delayMs);

    return Mono.delay(Duration.ofMillis(delayMs))
        .map(
            tick ->
                ResponseEntity.ok(
                    Map.of(
                        "message", "Delayed response from EchoUnit!",
                        "instanceId", instanceName,
                        "port", serverPort,
                        "timestamp", LocalDateTime.now().toString(),
                        "requestCount", count,
                        "delayMs", delayMs)));
  }

  /**
   * Long-lived session - simulates a connection that remains active
   *
   * @param durationSeconds session duration in seconds (default 30)
   */
  @GetMapping("/echo/session")
  public Mono<ResponseEntity<Map<String, Object>>> createSession(
      @RequestParam(defaultValue = "30") int durationSeconds) {
    String sessionId = UUID.randomUUID().toString();
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(durationSeconds);
    activeSessions.put(sessionId, expiresAt);

    log.info(
        "Created new session {} with duration {}s. Active sessions: {}",
        sessionId,
        durationSeconds,
        activeSessions.size());

    // Schedule session removal after expiration
    Mono.delay(Duration.ofSeconds(durationSeconds))
        .subscribe(
            tick -> {
              activeSessions.remove(sessionId);
              log.info(
                  "Session {} expired. Remaining sessions: {}", sessionId, activeSessions.size());
            });

    return Mono.just(
        ResponseEntity.ok(
            Map.of(
                "message",
                "Session created",
                "instanceId",
                instanceName,
                "port",
                serverPort,
                "sessionId",
                sessionId,
                "activeSessions",
                activeSessions.size(),
                "expiresAt",
                expiresAt.toString())));
  }

  /** Get information about current active sessions */
  @GetMapping("/echo/sessions")
  public ResponseEntity<Map<String, Object>> getActiveSessions() {
    // Clean up expired sessions
    LocalDateTime now = LocalDateTime.now();
    activeSessions.entrySet().removeIf(entry -> entry.getValue().isBefore(now));

    return ResponseEntity.ok(
        Map.of(
            "instanceId", instanceName,
            "port", serverPort,
            "activeSessions", activeSessions.size(),
            "sessionDetails", activeSessions));
  }

  /**
   * Request with variable CPU load
   *
   * @param intensity load intensity from 1 to 10 (default 5)
   */
  @GetMapping("/echo/cpu")
  public ResponseEntity<Map<String, Object>> cpuIntensiveTask(
      @RequestParam(defaultValue = "5") int intensity) {
    int count = requestCounter.incrementAndGet();
    log.info("Received CPU-intensive request #{} with intensity {}", count, intensity);

    // Limit intensity between 1 and 10
    int workload = Math.max(1, Math.min(10, intensity));

    // Perform "heavy" calculations
    long startTime = System.currentTimeMillis();
    double result = 0;
    int iterations = workload * 10_000_000;

    for (int i = 0; i < iterations; i++) {
      result += Math.sin(i) * Math.cos(i);
    }

    long duration = System.currentTimeMillis() - startTime;

    return ResponseEntity.ok(
        Map.of(
            "message", "CPU task completed",
            "instanceId", instanceName,
            "port", serverPort,
            "intensity", workload,
            "iterations", iterations,
            "duration", duration,
            "result", String.format("%.2f", result)));
  }

  /**
   * Request with error probability
   *
   * @param errorProbability probability of error from 0 to 1 (default 0.3)
   */
  @GetMapping("/echo/error")
  public ResponseEntity<?> errorProne(@RequestParam(defaultValue = "0.3") double errorProbability) {
    int count = requestCounter.incrementAndGet();
    log.info("Received error-prone request #{} with error probability {}", count, errorProbability);

    // Limit probability between 0 and 1
    double probability = Math.max(0, Math.min(1, errorProbability));

    if (random.nextDouble() < probability) {
      log.info("Returning error response");
      return ResponseEntity.internalServerError()
          .body(
              Map.of(
                  "error", "Random error occurred",
                  "instanceId", instanceName,
                  "port", serverPort,
                  "errorProbability", probability));
    } else {
      return ResponseEntity.ok(
          Map.of(
              "message", "Success (no error occurred)",
              "instanceId", instanceName,
              "port", serverPort,
              "errorProbability", probability));
    }
  }

  /**
   * Request with variable response size
   *
   * @param sizeKb response size in kilobytes (default 10)
   */
  @GetMapping("/echo/payload")
  public ResponseEntity<Map<String, Object>> largePayload(
      @RequestParam(defaultValue = "10") int sizeKb) {
    int count = requestCounter.incrementAndGet();
    log.info("Received large payload request #{} with size {}KB", count, sizeKb);

    // Limit size between 1 and 1024 KB (1 MB)
    int size = Math.max(1, Math.min(1024, sizeKb));

    // Generate string of required size
    StringBuilder payload = new StringBuilder(size * 1024);
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < 1024; j++) {
        payload.append((char) ('A' + random.nextInt(26)));
      }
    }

    return ResponseEntity.ok(
        Map.of(
            "message", "Large payload response",
            "instanceId", instanceName,
            "port", serverPort,
            "sizeKb", size,
            "payload", payload.toString()));
  }

  /** Health check endpoint */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    return ResponseEntity.ok(
        Map.of(
            "status",
            "UP",
            "instanceId",
            instanceName,
            "port",
            serverPort,
            "activeSessions",
            activeSessions.size(),
            "requestCount",
            requestCounter.get(),
            "timestamp",
            LocalDateTime.now().toString()));
  }
}
