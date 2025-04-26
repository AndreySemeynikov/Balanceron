package com.andrey.semeynikov.balanceron.controller;

import com.andrey.semeynikov.balanceron.model.RegistrationRequest;
import com.andrey.semeynikov.balanceron.model.RegistrationResponse;
import com.andrey.semeynikov.balanceron.model.ServiceInstance;
import com.andrey.semeynikov.balanceron.service.InstanceRegistryService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/registry")
@RequiredArgsConstructor
public class RegistryController {
  private final InstanceRegistryService registryService;

  @PostMapping("/register")
  public Mono<RegistrationResponse> registerInstance(@RequestBody RegistrationRequest request) {
    ServiceInstance instance = new ServiceInstance();
    instance.setId(request.instanceId());
    instance.setServiceName(request.instanceName());
    instance.setHost(request.host());
    instance.setPort(request.port());
    instance.setWeight(request.weight());
    instance.setLastHeartbeat(Instant.now());

    registryService.registerInstance(instance);

    RegistrationResponse response =
        new RegistrationResponse("SUCCESS", "Instance registered successfully");

    return Mono.just(response);
  }

  @PutMapping("/heartbeat/{instanceId}")
  public Mono<String> heartbeat(@PathVariable String instanceId) {
    registryService.updateHeartbeat(instanceId);
    return Mono.just("OK");
  }

  @DeleteMapping("/instances/{instanceId}")
  public Mono<String> deregisterInstance(@PathVariable String instanceId) {
    registryService.removeInstance(instanceId);
    return Mono.just("Instance deregistered successfully");
  }

  @GetMapping("/instances")
  public Mono<List<ServiceInstance>> getActiveInstances() {
    return Mono.just(registryService.getActiveInstances());
  }
}
