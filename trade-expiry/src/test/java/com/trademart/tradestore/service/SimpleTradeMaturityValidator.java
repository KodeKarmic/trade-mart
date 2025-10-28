package com.trademart.tradestore.service;

import com.trademart.tradeexpiry.service.TradeMaturityValidator;
import com.trademart.tradestore.exception.TradeValidationException;
import com.trademart.tradestore.service.ClockService;

import java.time.LocalDate;

/**
 * Legacy in-module implementation; kept temporarily to ensure beans wire during
 * refactor. Once
 * trade-expiry module is fully authoritative this class can be removed.
 * Marked @Primary=false via
 * ordering default; trade-expiry implementation should win if on classpath.
 */
@Deprecated(forRemoval = true)
public class SimpleTradeMaturityValidator implements TradeMaturityValidator {
  private final ClockService clockService;

  public SimpleTradeMaturityValidator(ClockService clockService) {
    this.clockService = clockService;
  }

  @Override
  public void validate(LocalDate maturityDate) {
    if (maturityDate == null)
      return;
    LocalDate today = LocalDate.now(clockService.getClock());
    if (maturityDate.isBefore(today)) {
      throw new TradeValidationException("maturity date is in the past");
    }
  }
}
