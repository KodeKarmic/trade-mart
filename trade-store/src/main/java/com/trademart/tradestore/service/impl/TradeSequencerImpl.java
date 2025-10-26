package com.trademart.tradestore.service.impl;

import com.trademart.tradestore.service.TradeSequencer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeSequencerImpl implements TradeSequencer {

  private final JdbcTemplate jdbcTemplate;

  public TradeSequencerImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public long nextSequence() {
    // Use a DB sequence to generate a monotonic ingest id. Flyway migration
    // will create `trade_ingest_seq`.
    Long v = jdbcTemplate.queryForObject("SELECT nextval('trade_ingest_seq')", Long.class);
    return v == null ? 0L : v.longValue();
  }
}
