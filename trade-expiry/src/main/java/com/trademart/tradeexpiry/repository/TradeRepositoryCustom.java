package com.trademart.tradeexpiry.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.trademart.tradeexpiry.model.TradeEntity;

public interface TradeRepositoryCustom {
  TradeEntity upsertTrade(
      String tradeId,
      Integer version,
      BigDecimal price,
      Integer quantity,
      LocalDate maturityDate,
      Long ingestSequence,
      String status);
}
