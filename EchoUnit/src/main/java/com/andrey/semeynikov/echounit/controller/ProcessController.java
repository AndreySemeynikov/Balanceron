package com.andrey.semeynikov.echounit.controller;

import com.andrey.semeynikov.echounit.model.ResponseDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProcessController {

    UUID instanceId = UUID.randomUUID();

    @GetMapping("/process")
    public ResponseEntity<ResponseDto> processRequest() {
        log.info("Request to process data");
        return ResponseEntity.ok(new ResponseDto(instanceId));
    }
}
