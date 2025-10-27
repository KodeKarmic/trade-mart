package com.trademart.tradestore.service;

import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.model.TradeStatus;
import com.trademart.tradestore.mongo.TradeHistory;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeExpiryService {
  private static final Logger log = LoggerFactory.getLogger(TradeExpiryService.class);
  private final TradeRepository tradeRepository;
  private final TradeHistoryRepository tradeHistoryRepository;
  private final ClockService clockService;
  public TradeExpiryService(TradeRepository tradeRepository, TradeHistoryRepository tradeHistoryRepository, ClockService clockService) {
    this.tradeRepository = tradeRepository;
    this.tradeHistoryRepository = tradeHistoryRepository;
    this.clockService = clockService;
  }

  @Transactional
  public List<TradeEntity> expireDueTrades() {
    LocalDate todayUtc = LocalDate.ofInstant(clockService.nowUtc(), java.time.ZoneOffset.UTC);
    log.info("expiry job: todayUtc={}", todayUtc);
    List<TradeEntity> due = tradeRepository.findByStatusAndMaturityDateBefore(TradeStatus.ACTIVE, todayUtc);
    log.info("expiry job: found {} due trades", due == null ? 0 : due.size());
    if (due == null || due.isEmpty()) return List.of();

    List<TradeEntity> updated = new ArrayList<>();
    List<TradeHistory> histories = new ArrayList<>();
    Instant now = Instant.now();

    for (TradeEntity t : due) {
      TradeEntity before = new TradeEntity(t.getTradeId(), t.getVersion(), t.getPrice(), t.getQuantity(), t.getMaturityDate(), t.getStatus());
      t.setStatus(TradeStatus.EXPIRED);
      t.setUpdatedAt(now);
      updated.add(t);

      TradeHistory h = new TradeHistory();
      h.setTradeId(t.getTradeId());
      h.setVersion(t.getVersion());
      h.setChangeType("EXPIRE");
      h.setBefore(Map.of("tradeId", before.getTradeId(), "version", before.getVersion(), "status", before.getStatus()));
      h.setAfter(Map.of("tradeId", t.getTradeId(), "version", t.getVersion(), "status", t.getStatus()));
      h.setActor("system");
      h.setTimestamp(now);
      histories.add(h);
    }

    tradeRepository.saveAll(updated);
    log.info("expiry job: updated {} trades", updated.size());
    try { tradeHistoryRepository.saveAll(histories); } catch (Exception e) { log.warn("failed to persist trade history for expiry job: {}", e.getMessage()); }
    return updated;
  }
}
