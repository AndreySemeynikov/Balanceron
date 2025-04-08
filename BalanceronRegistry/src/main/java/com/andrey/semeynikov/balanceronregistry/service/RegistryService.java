package com.andrey.semeynikov.balanceronregistry.service;

import com.andrey.semeynikov.balanceronregistry.model.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class RegistryService {

    static final long HEARTBEAT_TIMEOUT = 30_000;

    Map<String, ServiceInstance> instances = new ConcurrentHashMap<>();

    public void register(ServiceInstance instance) {
        instance.updateHeartbeat();
        instances.put(instance.getInstanceId(), instance);
        log.info("Registered instance {} at {}:{}", instance.getInstanceId(), instance.getAddress(), instance.getPort());
    }

    public boolean receiveHeartbeat(String instanceId) {
        ServiceInstance instance = instances.get(instanceId);
        if (instance != null) {
            instance.updateHeartbeat();
            log.info("Heartbeat received from {}", instanceId);
            return true;
        } else {
            log.warn("Heartbeat received from unknown instance {}", instanceId);
            return false;
        }
    }

    public List<ServiceInstance> getActiveInstances() {
        return new ArrayList<>(instances.values());
    }

    public void removeInactiveInstances() {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, ServiceInstance> entry : instances.entrySet()) {
            if (Duration.between(entry.getValue().getLastHeartBeat(), Instant.now()).toMillis() > HEARTBEAT_TIMEOUT) {
                toRemove.add(entry.getKey());
            }
        }

        for (String id : toRemove) {
            instances.remove(id);
            log.warn("Removed inactive instance {}", id);
        }
    }
}
