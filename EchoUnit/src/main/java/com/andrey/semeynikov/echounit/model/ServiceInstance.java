package com.andrey.semeynikov.echounit.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = PRIVATE)
public class ServiceInstance {

    @NotNull
    String instanceId;

    @NotNull
    String address;

    int port;

    Instant lastHeartBeat;

    public ServiceInstance(String instanceId, String address, int port) {
        this.instanceId = instanceId;
        this.address = address;
        this.port = port;
        this.lastHeartBeat = Instant.now();
    }

    public void updateHeartbeat() {
        this.lastHeartBeat = Instant.now();
    }
}