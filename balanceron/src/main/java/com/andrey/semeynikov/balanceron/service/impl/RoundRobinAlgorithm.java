package com.andrey.semeynikov.balanceron.service.impl;

import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import com.andrey.semeynikov.balanceron.service.LoadBalancingAlgorithm;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinAlgorithm implements LoadBalancingAlgorithm {

  private final AtomicInteger counter = new AtomicInteger(0);

  @Override
  public ServiceInstance chooseInstance(List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }
    int index = counter.getAndIncrement() % instances.size();
    if (counter.get() > 10000) {
      counter.set(0);
    }
    return instances.get(index);
  }

  @Override
  public String getName() {
    return "Round Robin";
  }

  @Override
  public String getDescription() {
    return "Distributes sequentially across all available instances";
  }
}
