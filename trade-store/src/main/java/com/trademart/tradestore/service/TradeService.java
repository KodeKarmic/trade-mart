package com.trademart.tradestore.service;

import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;

public interface TradeService {
  TradeEntity createOrUpdateTrade(TradeDto dto);
}
