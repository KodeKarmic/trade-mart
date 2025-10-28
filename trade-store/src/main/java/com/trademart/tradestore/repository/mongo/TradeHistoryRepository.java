package com.trademart.tradestore.repository.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.trademart.tradestore.mongo.TradeHistory;

public interface TradeHistoryRepository extends MongoRepository<TradeHistory, String> {
  List<TradeHistory> findByTradeIdOrderByVersionDesc(String tradeId);
}
