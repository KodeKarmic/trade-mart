package com.trademart.tradestore.service;

import java.time.LocalDate;

/**
 * Validates maturity date rules. Implementations should throw an unchecked
 * exception (runtime)
 * relevant to the calling context if invalid. To avoid a dependency on
 * trade-store DTOs this
 * module only receives the maturity date directly.
 */
public interface TradeMaturityValidator {
  void validate(LocalDate maturityDate);
}
