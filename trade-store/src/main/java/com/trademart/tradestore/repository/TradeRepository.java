package com.trademart.tradestore.repository;

import com.trademart.tradestore.model.TradeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<TradeEntity, Long>, TradeRepositoryCustom {
  Optional<TradeEntity> findByTradeId(String tradeId);
}
