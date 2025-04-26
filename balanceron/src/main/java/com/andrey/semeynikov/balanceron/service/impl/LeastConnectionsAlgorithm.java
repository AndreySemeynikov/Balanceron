package com.andrey.semeynikov.balanceron.service.impl;

import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import com.andrey.semeynikov.balanceron.service.LoadBalancingAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LeastConnectionsAlgorithm implements LoadBalancingAlgorithm {

  private final AtomicInteger counter = new AtomicInteger(0);

  @Override
  public ServiceInstance chooseInstance(List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }

    return instances.stream()
        .min(Comparator.comparingInt(instance -> instance.getActiveConnections().get()))
        .orElse(instances.getFirst());
  }

  @Override
  public String getName() {
    return "Least Connections";
  }

  @Override
  public String getDescription() {
    return "Selects the instance with the fewest active connections";
  }
}
