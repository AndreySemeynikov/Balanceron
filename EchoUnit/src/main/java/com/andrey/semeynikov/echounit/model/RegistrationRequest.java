package com.andrey.semeynikov.echounit.model;

public record RegistrationRequest(
    String instanceId, String instanceName, String host, int port, int weight) {}
