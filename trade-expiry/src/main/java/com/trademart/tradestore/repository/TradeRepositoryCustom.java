package com.trademart.tradestore.repository;

import com.trademart.tradestore.model.TradeEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

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
