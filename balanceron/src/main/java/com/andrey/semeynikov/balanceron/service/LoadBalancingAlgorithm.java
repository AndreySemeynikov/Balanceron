package com.andrey.semeynikov.balanceron.service;

import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import java.util.List;

public interface LoadBalancingAlgorithm {

  ServiceInstance chooseInstance(List<ServiceInstance> instances);

  String getName();

  String getDescription();
}
