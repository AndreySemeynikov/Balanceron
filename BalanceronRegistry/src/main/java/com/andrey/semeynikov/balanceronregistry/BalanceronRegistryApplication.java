package com.andrey.semeynikov.balanceronregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BalanceronRegistryApplication {

  public static void main(String[] args) {
    SpringApplication.run(BalanceronRegistryApplication.class, args);
  }
}
