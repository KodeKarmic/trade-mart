package com.trademart.tradestore.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.model.TradeStatus;

public interface TradeRepository extends JpaRepository<TradeEntity, Long>, TradeRepositoryCustom {
        Optional<TradeEntity> findByTradeId(String tradeId);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(t.version) FROM TradeEntity t WHERE t.tradeId = :tradeId")
        Integer findMaxVersionByTradeId(
                        @org.springframework.data.repository.query.Param("tradeId") String tradeId);

        java.util.List<TradeEntity> findByStatusAndMaturityDateBefore(
                        TradeStatus status, java.time.LocalDate date);
}
