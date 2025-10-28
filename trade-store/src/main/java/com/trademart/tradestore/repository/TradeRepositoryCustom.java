package com.trademart.tradestore.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.trademart.tradestore.model.TradeEntity;

public interface TradeRepositoryCustom {
  /**
   * Perform an atomic upsert of a trade row using Postgres ON CONFLICT DO UPDATE.
   * Returns the
   * persisted TradeEntity after the operation.
   */
  TradeEntity upsertTrade(
      String tradeId,
      Integer version,
      BigDecimal price,
      Integer quantity,
      LocalDate maturityDate,
      Long ingestSequence,
      String status);
}
