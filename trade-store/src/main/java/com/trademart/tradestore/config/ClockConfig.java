package com.trademart.tradestore.config;

import java.time.Clock;

// Clock bean moved to trade-clock; imports removed to keep this file inert.

/**
 * Legacy clock provider kept for compatibility during migration. The canonical Clock bean and
 * `ClockService` now live in the `trade-clock` module. This class is intentionally not
 * a @Configuration so it will not register beans.
 */
@Deprecated(forRemoval = true)
public class ClockConfig {

  /**
   * System UTC clock to be injected where a Clock is required. Using an injectable Clock makes
   * services testable by allowing a fixed clock to be provided in tests.
   */
  // No-op helper kept for reference during migration. Use ClockService from
  // the trade-clock module instead.
  public Clock systemUtcClock() {
    return Clock.systemUTC();
  }
}
