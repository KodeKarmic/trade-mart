package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.service.impl.SimpleTradeMaturityValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TradeMaturityValidatorTest {

  @Test
  void rejects_past_maturity() {
    Clock fixed = Clock.fixed(Instant.parse("2025-10-27T00:00:00Z"), ZoneOffset.UTC);
    com.trademart.tradestore.service.ClockService cs = new com.trademart.tradestore.service.ClockService(fixed);
    SimpleTradeMaturityValidator v = new SimpleTradeMaturityValidator(cs);
    TradeDto dto = new TradeDto();
    dto.setTradeId("T-1");
    dto.setVersion(1);
    dto.setPrice(java.math.BigDecimal.TEN);
    dto.setMaturityDate(LocalDate.of(2025, 10, 26));

    assertThatThrownBy(() -> v.validate(dto)).isInstanceOf(TradeRejectedException.class)
        .hasMessageContaining("maturity date is in the past");
  }

  @Test
  void accepts_today_or_future_maturity() {
    Clock fixed = Clock.fixed(Instant.parse("2025-10-27T00:00:00Z"), ZoneOffset.UTC);
    com.trademart.tradestore.service.ClockService cs = new com.trademart.tradestore.service.ClockService(fixed);
    SimpleTradeMaturityValidator v = new SimpleTradeMaturityValidator(cs);
    TradeDto dto = new TradeDto();
    dto.setTradeId("T-2");
    dto.setVersion(1);
    dto.setPrice(java.math.BigDecimal.ONE);
    dto.setMaturityDate(LocalDate.of(2025, 10, 27));

    assertThatNoException().isThrownBy(() -> v.validate(dto));
  }
}
