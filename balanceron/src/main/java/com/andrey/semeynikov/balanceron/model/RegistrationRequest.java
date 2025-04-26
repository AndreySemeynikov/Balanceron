package com.andrey.semeynikov.balanceron.model;

public record RegistrationRequest(
    String instanceId, String instanceName, String host, int port, int weight) {}
