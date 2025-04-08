package com.andrey.semeynikov.balanceronregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CleanupTask {

    RegistryService registryService;

    @Scheduled(fixedDelay = 10_000)
    public void cleanDeadInstances() {
        log.debug("Running cleanup for inactive instances...");
        registryService.removeInactiveInstances();
    }
}