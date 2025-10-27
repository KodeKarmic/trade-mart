package com.trademart.tradestore.service;

import java.time.Clock;
import java.time.Instant;
// Deprecated compatibility shim; clock bean provided by trade-clock module.

@Deprecated(forRemoval = true)
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

  // Clock bean is provided by the `trade-clock` module. This class remains as a
  // deprecated compatibility shim and is not annotated as a component so it will
  // not participate in component scanning.
}
