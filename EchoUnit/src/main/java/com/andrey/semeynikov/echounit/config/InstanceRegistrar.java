package com.andrey.semeynikov.echounit.config;

import com.andrey.semeynikov.echounit.model.ServiceInstance;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE)
public class InstanceRegistrar {

    final RestTemplate restTemplate;
    final EchoUnitConfig config;

    @Value("${INSTANCE_ID:}")
    String instanceIdEnv;

    @Value("${server.port:8080}")
    int port;

    String instanceId;
    String address;

    @PostConstruct
    public void init() {
        this.instanceId = instanceIdEnv != null && !instanceIdEnv.isBlank()
            ? instanceIdEnv : UUID.randomUUID().toString();

        try {
            this.address = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.error("Unable to resolve local IP address: {}", e.getMessage(), e);
            this.address = "unknown";
        }

        register();
    }

    public void register() {
        ServiceInstance payload = new ServiceInstance(instanceId, address, port);
        try {
            restTemplate.postForEntity(config.getRegistryUrl() + "/register", payload, Void.class);
            log.info("Instance registered: instanceId={}, address={}, port={}, registry={}",
                instanceId, address, port, config.getRegistryUrl());
        } catch (Exception e) {
            log.error("Failed to register instance with registry {}: {}", config.getRegistryUrl(), e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 10_000)
    public void sendHeartbeat() {
        String url = config.getRegistryUrl() + "/heartbeat?instanceId=" + instanceId;
        try {
            restTemplate.postForEntity(url, null, Void.class);
            log.debug("Heartbeat sent: instanceId={}, address={}, port={}", instanceId, address, port);
        } catch (Exception e) {
            log.warn("Failed to send heartbeat for instanceId={} to registry {}: {}",
                instanceId, config.getRegistryUrl(), e.getMessage(), e);
        }
    }
}
