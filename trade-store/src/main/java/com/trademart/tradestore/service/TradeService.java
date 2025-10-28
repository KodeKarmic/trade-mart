package com.trademart.tradestore.service;

import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.model.TradeDto;

public interface TradeService {
  TradeEntity createOrUpdateTrade(TradeDto dto);
}
