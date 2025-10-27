package com.trademart.tradestore.expiry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.trademart.tradestore")
@EnableScheduling
public class TradeExpiryApplication {
  public static void main(String[] args) {
    SpringApplication.run(TradeExpiryApplication.class, args);
  }
}
