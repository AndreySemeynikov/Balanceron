package com.andrey.semeynikov.balanceron.service;

import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service responsible for forwarding HTTP requests to the selected service instance. It handles the
 * actual HTTP communication, including request transformation, connection tracking, and response
 * handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpForwardService {

  private final WebClient.Builder webClientBuilder;

  // Timeout for forwarded requests in seconds
  private static final int REQUEST_TIMEOUT_SECONDS = 60;

  /**
   * Forwards an HTTP request to the specified service instance.
   *
   * @param request The original HTTP request
   * @param instance The target service instance
   * @return A Mono containing the response from the service instance
   */
  public Mono<ResponseEntity<byte[]>> forwardRequest(
      ServerHttpRequest request, ServiceInstance instance) {
    // Start metrics timer
    long startTime = System.currentTimeMillis();

    // Increment active connections counter
    instance.getActiveConnections().incrementAndGet();

    String targetUrl = buildTargetUrl(request, instance);
    HttpMethod method = request.getMethod();

    log.debug("Forwarding {} request to: {}", method, targetUrl);

    // Prepare headers
    HttpHeaders headers = new HttpHeaders();
    request
        .getHeaders()
        .forEach(
            (name, values) -> {
              // Skip hop-by-hop headers
              if (!isHopByHopHeader(name)) {
                headers.addAll(name, values);
              }
            });

    // Add custom headers
    headers.add("X-Forwarded-By", "Balanceron");
    headers.add("X-Forwarded-For", getClientIp(request));

    // Create WebClient for this request
    WebClient client = webClientBuilder.build();

    // Create request specification
    WebClient.RequestBodySpec requestBodySpec =
        client.method(method).uri(targetUrl).headers(httpHeaders -> httpHeaders.addAll(headers));

    // Execute request with or without body
    Mono<ResponseEntity<byte[]>> responseMono;

    if (method == HttpMethod.GET || method == HttpMethod.HEAD) {
      responseMono = executeRequest(requestBodySpec, instance);
    } else {
      // For requests with body (POST, PUT, etc.)
      responseMono =
          request
              .getBody()
              .collectList()
              .flatMap(
                  dataBuffers -> {
                    byte[] body = aggregateBody(dataBuffers);
                    return executeRequest(requestBodySpec.bodyValue(body), instance);
                  });
    }

    // Add timeout and error handling
    return responseMono
        .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
        .doOnSuccess(
            response -> {
              long duration = System.currentTimeMillis() - startTime;
              log.debug(
                  "Request to {} completed in {}ms with status: {}",
                  targetUrl,
                  duration,
                  response.getStatusCode());
            })
        .doOnError(
            error -> {
              log.error("Error forwarding request to {}: {}", targetUrl, error.getMessage());
            })
        .doFinally(
            signalType -> {
              // Decrement active connections counter
              instance.getActiveConnections().decrementAndGet();
            });
  }

  /**
   * Executes the request and returns the response.
   *
   * @param requestSpec The WebClient request specification
   * @param instance The target service instance
   * @return A Mono containing the response
   */
  private Mono<ResponseEntity<byte[]>> executeRequest(
      WebClient.RequestHeadersSpec<?> requestSpec, ServiceInstance instance) {
    return requestSpec
        .retrieve()
        .toEntity(byte[].class)
        .doOnSubscribe(s -> log.debug("Sending request to instance: {}", instance.getId()));
  }


  /**
   * Builds the target URL for the forwarded request.
   *
   * @param request The original request
   * @param instance The target service instance
   * @return The complete target URL
   */
  private String buildTargetUrl(ServerHttpRequest request, ServiceInstance instance) {
    return String.format(
        "http://%s:%d%s%s",
        instance.getHost(),
        instance.getPort(),
        request.getPath().value(),
        request.getURI().getRawQuery() != null ? "?" + request.getURI().getRawQuery() : "");
  }

  /**
   * Aggregates DataBuffer list into a byte array.
   *
   * @param dataBuffers List of DataBuffer objects
   * @return Aggregated byte array
   */
  private byte[] aggregateBody(java.util.List<DataBuffer> dataBuffers) {
    if (dataBuffers.isEmpty()) {
      return new byte[0];
    }

    return dataBuffers.stream()
        .map(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);
              return bytes;
            })
        .reduce(
            new byte[0],
            (acc, bytes) -> {
              byte[] result = new byte[acc.length + bytes.length];
              System.arraycopy(acc, 0, result, 0, acc.length);
              System.arraycopy(bytes, 0, result, acc.length, bytes.length);
              return result;
            });
  }

  /**
   * Checks if a header is a hop-by-hop header that should not be forwarded.
   *
   * @param headerName The name of the header
   * @return true if the header is hop-by-hop, false otherwise
   */
  private boolean isHopByHopHeader(String headerName) {
    String name = headerName.toLowerCase();
    return name.equals("connection")
        || name.equals("keep-alive")
        || name.equals("proxy-authenticate")
        || name.equals("proxy-authorization")
        || name.equals("te")
        || name.equals("trailers")
        || name.equals("transfer-encoding")
        || name.equals("upgrade");
  }

  /**
   * Extracts the client IP address from the request.
   *
   * @param request The HTTP request
   * @return The client IP address
   */
  private String getClientIp(ServerHttpRequest request) {
    HttpHeaders headers = request.getHeaders();

    String ip = headers.getFirst("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = headers.getFirst("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = headers.getFirst("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip =
          request.getRemoteAddress() != null
              ? request.getRemoteAddress().getHostString()
              : "unknown";
    }

    // If X-Forwarded-For contains multiple IPs, take the first one
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }

    return ip;
  }
}
