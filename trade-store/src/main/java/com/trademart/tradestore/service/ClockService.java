package com.trademart.tradestore.service;

import java.time.Clock;
import java.time.Instant;
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
}
