package com.trademart.tradestore.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ClockIntegrationTest {

  @Autowired
  Clock clock;

  @Test
  void clockBean_isSystemUtc() {
    // The Clock bean provided by ClockConfig should use UTC as its zone.
    assertEquals(ZoneOffset.UTC, clock.getZone());
  }
}
