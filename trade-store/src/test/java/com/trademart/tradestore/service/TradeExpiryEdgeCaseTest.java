package com.trademart.tradestore.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class TradeExpiryEdgeCaseTest {

  @Test
  void maturityExactlyToday_isNotBeforeToday() {
    LocalDate today = LocalDate.now();
    LocalDate maturity = LocalDate.of(today.getYear(), today.getMonth(), today.getDayOfMonth());

    // sanity: maturity equals today
    assertThat(maturity).isEqualTo(today);

    // main assertion: maturity.isBefore(today) should be false when equal
    assertThat(maturity.isBefore(today)).isFalse();
  }
}
