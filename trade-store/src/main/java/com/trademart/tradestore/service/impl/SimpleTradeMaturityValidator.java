package com.trademart.tradestore.service.impl;

import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.service.ClockService;
import com.trademart.tradestore.service.TradeMaturityValidator;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class SimpleTradeMaturityValidator implements TradeMaturityValidator {

  private final ClockService clockService;

  public SimpleTradeMaturityValidator(ClockService clockService) {
    this.clockService = clockService;
  }

  @Override
  public void validate(TradeDto incoming) throws TradeRejectedException {
    if (incoming == null)
      return;
    LocalDate today = LocalDate.now(clockService.getClock());
    if (incoming.getMaturityDate() != null && incoming.getMaturityDate().isBefore(today)) {
      throw new TradeRejectedException("maturity date is in the past");
    }
  }
}
