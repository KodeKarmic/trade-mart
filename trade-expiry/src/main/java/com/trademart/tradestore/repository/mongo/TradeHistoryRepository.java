package com.trademart.tradestore.repository.mongo;

import com.trademart.tradestore.mongo.TradeHistory;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TradeHistoryRepository extends MongoRepository<TradeHistory, String> {
  List<TradeHistory> findByTradeIdOrderByVersionDesc(String tradeId);
}
