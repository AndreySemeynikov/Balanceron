package com.andrey.semeynikov.balanceronregistry.controller;

import com.andrey.semeynikov.balanceronregistry.model.ServiceInstance;
import com.andrey.semeynikov.balanceronregistry.service.RegistryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RestController
@RequestMapping("/registry")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class RegistryController {

    RegistryService registryService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody ServiceInstance instance) {
        log.info("Request to register instance");
        registryService.register(instance);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestParam String instanceId) {
        boolean found = registryService.receiveHeartbeat(instanceId);
        return found ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/instances")
    public ResponseEntity<List<ServiceInstance>> getActiveInstances() {
        return ResponseEntity.ok(registryService.getActiveInstances());
    }
}