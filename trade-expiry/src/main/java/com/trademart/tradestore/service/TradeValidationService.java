package com.trademart.tradestore.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

/** Basic validation rules for incoming trades (moved from trade-store). */
@Service
public class TradeValidationService {
  private final ClockService clockService;

  public TradeValidationService(ClockService clockService) {
    this.clockService = clockService;
  }

  public void validateForIngest(String tradeId, Integer version, java.time.LocalDate maturityDate) {
    if (tradeId == null || tradeId.isBlank()) {
      throw new IllegalArgumentException("tradeId is required");
    }
    if (version == null || version < 0) {
      throw new IllegalArgumentException("version must be >= 0");
    }
    if (maturityDate != null) {
      ZonedDateTime nowUtc = ZonedDateTime.ofInstant(clockService.nowUtc(), ZoneId.of("UTC"));
      java.time.LocalDate todayUtc = nowUtc.toLocalDate();
      if (maturityDate.isBefore(todayUtc)) {
        throw new IllegalArgumentException("maturityDate must not be in the past");
      }
    }
  }
}
