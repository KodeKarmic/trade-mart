package com.trademart.tradestore.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import com.trademart.tradeexpiry.service.impl.SimpleTradeMaturityValidator;

class TradeMaturityValidatorTest {

  @Test
  void rejects_past_maturity() {
    Clock fixed = Clock.fixed(Instant.parse("2025-10-27T00:00:00Z"), ZoneOffset.UTC);
    com.trademart.tradestore.service.ClockService cs = new com.trademart.tradestore.service.ClockService(fixed);
    SimpleTradeMaturityValidator v = new SimpleTradeMaturityValidator(cs);
    LocalDate maturity = LocalDate.of(2025, 10, 26);

    assertThatThrownBy(() -> v.validate(maturity))
        .isInstanceOf(com.trademart.tradestore.exception.TradeValidationException.class)
        .hasMessageContaining("maturity date is in the past");
  }

  @Test
  void accepts_today_or_future_maturity() {
    Clock fixed = Clock.fixed(Instant.parse("2025-10-27T00:00:00Z"), ZoneOffset.UTC);
    com.trademart.tradestore.service.ClockService cs = new com.trademart.tradestore.service.ClockService(fixed);
    SimpleTradeMaturityValidator v = new SimpleTradeMaturityValidator(cs);
    LocalDate maturityToday = LocalDate.of(2025, 10, 27);

    assertThatNoException().isThrownBy(() -> v.validate(maturityToday));
  }
}
