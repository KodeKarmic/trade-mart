package com.trademart.tradestore.service;

import com.trademart.tradestore.exception.TradeValidationException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

/**
 * Basic validation rules for incoming trades. This is an example of typed
 * exceptions
 * and how to use them in the codebase.
 */
@Service
public class TradeValidationService {

  private final ClockService clockService;

  public TradeValidationService(ClockService clockService) {
    this.clockService = clockService;
  }

  public void validateForIngest(String tradeId, Integer version, java.time.LocalDate maturityDate) {
    if (tradeId == null || tradeId.isBlank()) {
      throw new TradeValidationException("tradeId is required");
    }
    if (version == null || version < 0) {
      throw new TradeValidationException("version must be >= 0");
    }
    if (maturityDate != null) {
      ZonedDateTime nowUtc = ZonedDateTime.ofInstant(clockService.nowUtc(), ZoneId.of("UTC"));
      java.time.LocalDate todayUtc = nowUtc.toLocalDate();
      if (maturityDate.isBefore(todayUtc)) {
        throw new TradeValidationException("maturityDate must not be in the past");
      }
    }
  }
}
