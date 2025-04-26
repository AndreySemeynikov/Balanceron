package com.andrey.semeynikov.balanceron.service.impl;

import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import com.andrey.semeynikov.balanceron.service.LoadBalancingAlgorithm;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WeightedRoundRobinAlgorithm implements LoadBalancingAlgorithm {

  private final AtomicInteger counter = new AtomicInteger(0);

  @Override
  public ServiceInstance chooseInstance(List<ServiceInstance> instances) {
    if (instances == null || instances.isEmpty()) {
      return null;
    }

    int totalWeight = instances.stream().mapToInt(ServiceInstance::getWeight).sum();

    if (totalWeight <= 0) {
      int index = counter.getAndIncrement() % instances.size();
      return instances.get(index);
    }

    int position = counter.getAndIncrement() % totalWeight;

    if (counter.get() > 10000) counter.set(0);

    int weightSum = 0;
    for (ServiceInstance instance : instances) {
      weightSum += instance.getWeight();
      if (position < weightSum) return instance;
    }
    return instances.getFirst();
  }

  @Override
  public String getName() {
    return "Weighted Round Robin";
  }

  @Override
  public String getDescription() {
    return "Distributes requests between instances based on their weights";
  }
}
