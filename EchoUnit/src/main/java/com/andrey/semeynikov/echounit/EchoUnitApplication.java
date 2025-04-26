package com.andrey.semeynikov.echounit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EchoUnitApplication {

  public static void main(String[] args) {
    SpringApplication.run(EchoUnitApplication.class, args);
  }
}
