package com.andrey.semeynikov.balanceron.service;

import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for selecting a service instance based on the current load balancing
 * algorithm. It works with the AlgorithmManager to get the current algorithm and the
 * InstanceRegistryService to get the list of available instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoadBalancingService {

  private final AlgorithmManager algorithmManager;
  private final InstanceRegistryService registryService;

  /**
   * Chooses a service instance for processing the request based on the current load balancing
   * algorithm.
   *
   * @return Optional containing the selected instance, or empty if no instances are available
   */
  public Optional<ServiceInstance> chooseInstance() {
    List<ServiceInstance> instances = registryService.getActiveInstances();

    if (instances.isEmpty()) {
      log.warn("No active instances available for load balancing");
      return Optional.empty();
    }

    log.debug(
        "Choosing instance from {} active instances using algorithm: {}",
        instances.size(),
        algorithmManager.getCurrentAlgorithmName());

    ServiceInstance chosen = algorithmManager.getCurrentAlgorithm().chooseInstance(instances);

    if (chosen != null) {
      log.debug(
          "Selected instance {} using algorithm {}",
          chosen.getId(),
          algorithmManager.getCurrentAlgorithmName());
      return Optional.of(chosen);
    } else {
      log.warn(
          "Algorithm {} failed to select an instance", algorithmManager.getCurrentAlgorithmName());
      return Optional.empty();
    }
  }
}
