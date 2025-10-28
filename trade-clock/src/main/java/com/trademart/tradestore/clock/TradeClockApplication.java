package com.trademart.tradestore.clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.trademart.tradestore")
public class TradeClockApplication {
  public static void main(String[] args) {
    SpringApplication.run(TradeClockApplication.class, args);
  }
}
