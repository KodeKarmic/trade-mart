package com.trademart.tradeexpiry.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.trademart.tradeexpiry.model.TradeEntity;
import com.trademart.tradeexpiry.service.TradeValidationService;

@Repository
public class TradeRepositoryImpl implements TradeRepositoryCustom {

  private final JdbcTemplate jdbcTemplate;
  private final TradeValidationService validator;

  @PersistenceContext
  private EntityManager em;

  public TradeRepositoryImpl(JdbcTemplate jdbcTemplate, TradeValidationService validator) {
    this.jdbcTemplate = jdbcTemplate;
    this.validator = validator;
  }

  @Override
  public TradeEntity upsertTrade(
      String tradeId,
      Integer version,
      BigDecimal price,
      Integer quantity,
      LocalDate maturityDate,
      Long ingestSequence,
      String status) {
    validator.validateForIngest(tradeId, version, maturityDate);
    final String sql = "INSERT INTO trades (trade_id, version, price, quantity, maturity_date, " +
        "status, ingest_sequence, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, now(), now()) " +
        "ON CONFLICT (trade_id) DO UPDATE SET " +
        "version = CASE WHEN EXCLUDED.version >= trades.version THEN EXCLUDED.version ELSE trades.version END, " +
        "price = CASE WHEN EXCLUDED.version >= trades.version THEN EXCLUDED.price ELSE trades.price END, " +
        "quantity = CASE WHEN EXCLUDED.version >= trades.version THEN EXCLUDED.quantity ELSE trades.quantity END, " +
        "maturity_date = CASE WHEN EXCLUDED.version >= trades.version THEN EXCLUDED.maturity_date ELSE trades.maturity_date END, "
        +
        "status = CASE WHEN EXCLUDED.version >= trades.version THEN EXCLUDED.status ELSE trades.status END, " +
        "ingest_sequence = CASE WHEN EXCLUDED.version >= trades.version THEN EXCLUDED.ingest_sequence ELSE trades.ingest_sequence END, "
        +
        "updated_at = CASE WHEN EXCLUDED.version >= trades.version THEN now() ELSE trades.updated_at END;";

    jdbcTemplate.update(sql, tradeId, version, price, quantity, maturityDate, status, ingestSequence);
    var q = em.createQuery("SELECT t FROM TradeEntity t WHERE t.tradeId = :tradeId", TradeEntity.class);
    q.setParameter("tradeId", tradeId);
    return q.getSingleResult();
  }
}
