package com.trademart.tradestore.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
public class ClockService {
  private final Clock clock;

  public ClockService(Clock clock) {
    this.clock = clock;
  }

  public Instant nowUtc() {
    return Instant.now(clock);
  }

  public Clock getClock() {
    return clock;
  }

  @Configuration
  public static class ClockConfig {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock systemClock() {
      return Clock.systemUTC();
    }
  }
}
