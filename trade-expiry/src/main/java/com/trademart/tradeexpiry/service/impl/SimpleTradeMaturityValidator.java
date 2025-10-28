package com.trademart.tradeexpiry.service.impl;

import com.trademart.tradestore.service.ClockService;
import com.trademart.tradeexpiry.service.TradeMaturityValidator;
import com.trademart.tradestore.exception.TradeValidationException;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
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
