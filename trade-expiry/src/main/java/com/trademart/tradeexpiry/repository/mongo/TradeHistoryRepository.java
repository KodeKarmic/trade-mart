package com.trademart.tradeexpiry.repository.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.trademart.tradeexpiry.mongo.TradeHistory;

public interface TradeHistoryRepository extends MongoRepository<TradeHistory, String> {
  List<TradeHistory> findByTradeIdOrderByVersionDesc(String tradeId);
}
