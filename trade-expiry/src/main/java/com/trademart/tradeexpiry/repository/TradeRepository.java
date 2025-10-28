package com.trademart.tradeexpiry.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.trademart.tradeexpiry.model.TradeEntity;
import com.trademart.tradeexpiry.model.TradeStatus;

public interface TradeRepository extends JpaRepository<TradeEntity, Long>, TradeRepositoryCustom {
  Optional<TradeEntity> findByTradeId(String tradeId);

  @Query("SELECT MAX(t.version) FROM TradeEntity t WHERE t.tradeId = :tradeId")
  Integer findMaxVersionByTradeId(@Param("tradeId") String tradeId);

  List<TradeEntity> findByStatusAndMaturityDateBefore(TradeStatus status, LocalDate date);
}
