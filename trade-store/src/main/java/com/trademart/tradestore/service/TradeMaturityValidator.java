package com.trademart.tradestore.service;

import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.model.TradeDto;

/**
 * Validates maturity date rules for incoming trades. Implementations should
 * throw
 * {@link TradeRejectedException} when the trade must be rejected.
 */
public interface TradeMaturityValidator {

  /**
   * Validate the incoming trade maturity date. Throws
   * {@link TradeRejectedException}
   * if the trade should be rejected.
   */
  void validate(TradeDto incoming) throws TradeRejectedException;
}
