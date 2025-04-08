package com.andrey.semeynikov.balanceronregistry.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(level = PRIVATE)
public class ServiceInstance {

    @NotNull
    String instanceId;

    @NotNull
    String address;

    int port;

    Instant lastHeartBeat;

    public void updateHeartbeat() {
        this.lastHeartBeat = Instant.now();
    }
}