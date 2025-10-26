package com.trademart.tradestore.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides infrastructure beans related to time.
 */
@Configuration
public class ClockConfig {

  /**
   * System UTC clock to be injected where a Clock is required. Using an
   * injectable Clock
   * makes services testable by allowing a fixed clock to be provided in tests.
   */
  @Bean
  public Clock systemUtcClock() {
    return Clock.systemUTC();
  }
}
