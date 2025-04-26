package com.andrey.semeynikov.balanceron.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;

@Data
public class ServiceInstance {
  private String id;
  private String host;
  private int port;
  private String serviceName;
  private Instant lastHeartbeat;
  private int weight = 1;
  private AtomicInteger activeConnections = new AtomicInteger(0);
}
