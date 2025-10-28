package com.trademart.tradestore.service;

import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;

/** Validate incoming trade version against an existing persisted trade. */
public interface TradeVersionValidator {

  /**
   * Validate the incoming dto against the existing trade entity. Should throw {@link
   * TradeRejectedException} if the incoming trade must be rejected.
   *
   * @param incoming incoming trade DTO
   * @param existing existing persisted trade (may be null)
   */
  void validate(TradeDto incoming, TradeEntity existing) throws TradeRejectedException;
}
