package com.trademart.tradestore.service.impl;

import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.service.TradeVersionValidator;
import org.springframework.stereotype.Component;

/**
 * Simple implementation of {@link TradeVersionValidator} used by the service when no custom
 * validator is provided. Rules: - if existing == null -> accept - if incoming.version == null ->
 * accept - if incoming.version < existing.version -> reject - otherwise accept
 */
@Component
public class SimpleTradeVersionValidator implements TradeVersionValidator {

  @Override
  public void validate(TradeDto incoming, TradeEntity existing) throws TradeRejectedException {
    if (existing == null) return;
    if (incoming == null) return;
    Integer inVer = incoming.getVersion();
    Integer exVer = existing.getVersion();
    if (inVer == null) return; // accept null incoming version
    if (exVer != null && inVer < exVer) {
      throw new TradeRejectedException("incoming version is lower than existing");
    }
  }
}
