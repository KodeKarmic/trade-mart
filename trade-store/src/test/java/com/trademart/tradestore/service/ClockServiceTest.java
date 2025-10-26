package com.trademart.tradestore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class ClockServiceTest {

  @Test
  void nowUtc_returnsInjectedClockInstant() {
    Instant fixedInstant = Instant.parse("2025-10-27T12:34:56Z");
    Clock fixed = Clock.fixed(fixedInstant, ZoneOffset.UTC);
    ClockService cs = new ClockService(fixed);

    assertEquals(fixedInstant, cs.nowUtc());
  }
}
