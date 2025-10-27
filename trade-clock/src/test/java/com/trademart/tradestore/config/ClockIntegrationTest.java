package com.trademart.tradestore.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Lightweight test verifying the Clock bean configuration produces a UTC system
 * clock.
 */
public class ClockIntegrationTest {
  @Test
  void clockBean_isSystemUtc() {
    com.trademart.tradestore.service.ClockService.ClockConfig cfg = new com.trademart.tradestore.service.ClockService.ClockConfig();
    Clock clock = cfg.systemClock();
    assertEquals(ZoneOffset.UTC, clock.getZone());
  }
}
