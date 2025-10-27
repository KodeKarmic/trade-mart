package com.trademart.tradestore.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

public class TradeValidationPropertyTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2025-10-27T00:00:00Z"), ZoneOffset.UTC);

  // Restrict the version domain to avoid jqwik mixing many extreme edge-cases
  // together.
  // This keeps property runs smaller and focused instead of raising 'tries'.
  @Property(tries = 200)
  void version_property_should_match_validation_rules(@ForAll("versions") int version) {
    TradeValidationService svc = new TradeValidationService(new ClockService(FIXED_CLOCK));

    if (version < 0) {
      assertThrows(
          com.trademart.tradestore.exception.TradeValidationException.class,
          () -> svc.validateForIngest("P1", version, null));
    } else {
      assertDoesNotThrow(() -> svc.validateForIngest("P1", version, null));
    }
  }

  // Limit tries and use a conservative date domain (no invalid day/month mixes)
  // so jqwik
  // won't heavily mix unusual edge-cases during generation.
  @Property(tries = 200)
  void date_property_should_reject_past_dates(@ForAll("sampleDates") LocalDate d) {
    TradeValidationService svc = new TradeValidationService(new ClockService(FIXED_CLOCK));
    LocalDate today = LocalDate.parse("2025-10-27");

    if (d.isBefore(today)) {
      assertThrows(
          com.trademart.tradestore.exception.TradeValidationException.class,
          () -> svc.validateForIngest("D1", 1, d));
    } else {
      assertDoesNotThrow(() -> svc.validateForIngest("D1", 1, d));
    }
  }

  @Provide
  Arbitrary<LocalDate> sampleDates() {
    // build LocalDate arbitrary by generating year/month/day avoiding invalid days
    // Narrow ranges so generated dates are reasonable and don't combine unlikely
    // extremes.
    var years = Arbitraries.integers().between(2023, 2027);
    var months = Arbitraries.integers().between(1, 12);
    var days = Arbitraries.integers().between(1, 28);

    return years.flatMap(y -> months.flatMap(m -> days.map(d -> LocalDate.of(y, m, d))));
  }

  @Provide
  Arbitrary<Integer> versions() {
    // Keep the integer domain modest to avoid jqwik mixing lots of edge-cases
    // together
    // (very large ints, negative extremes, etc.). This is our preferred approach
    // over
    // simply increasing the 'tries' count.
    return Arbitraries.integers().between(-1000, 1000);
  }
}
