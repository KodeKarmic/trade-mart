package com.trademart.tradestore.service.impl;

import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.model.TradeStatus;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import com.trademart.tradestore.service.TradeService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TradeServiceImpl implements TradeService {

  private final TradeRepository tradeRepository;
  private final TradeHistoryRepository tradeHistoryRepository;

  @Autowired
  public TradeServiceImpl(
      TradeRepository tradeRepository, TradeHistoryRepository tradeHistoryRepository) {
    this.tradeRepository = tradeRepository;
    this.tradeHistoryRepository = tradeHistoryRepository;
  }

  @Override
  public TradeEntity createOrUpdateTrade(TradeDto dto) {
    // Simple version validation and upsert behavior
    var existingOpt = tradeRepository.findByTradeId(dto.getTradeId());

    TradeEntity before = null;
    if (existingOpt.isPresent()) {
      before = existingOpt.get();
      if (dto.getVersion() != null && dto.getVersion() < before.getVersion()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "incoming version is lower than existing");
      }
    }

    // Maturity date validation: reject trades that are already expired
    if (dto.getMaturityDate() != null && dto.getMaturityDate().isBefore(LocalDate.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maturity date is in the past");
    }

    TradeEntity entity = existingOpt.orElseGet(() -> new TradeEntity());
    // map fields
    entity.setTradeId(dto.getTradeId());
    entity.setVersion(dto.getVersion());
    entity.setPrice(dto.getPrice());
    entity.setMaturityDate(dto.getMaturityDate());
    entity.setStatus(TradeStatus.ACTIVE);
    entity.setUpdatedAt(Instant.now());
    if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());

    // Use an atomic DB upsert to avoid concurrent-insert races.
    TradeEntity saved =
        tradeRepository.upsertTrade(
            entity.getTradeId(),
            entity.getVersion(),
            entity.getPrice(),
            entity.getQuantity(),
            entity.getMaturityDate(),
            entity.getStatus() == null ? null : entity.getStatus().name());

    // write history doc
    var hist = new com.trademart.tradestore.mongo.TradeHistory();
    hist.setTradeId(saved.getTradeId());
    hist.setVersion(saved.getVersion());
    hist.setChangeType(before == null ? "CREATE" : "UPDATE");
    Map<String, Object> beforeMap = new HashMap<>();
    if (before != null) {
      beforeMap.put("tradeId", before.getTradeId());
      beforeMap.put("version", before.getVersion());
      beforeMap.put("price", before.getPrice());
      beforeMap.put("maturityDate", before.getMaturityDate());
    }
    Map<String, Object> afterMap = new HashMap<>();
    afterMap.put("tradeId", saved.getTradeId());
    afterMap.put("version", saved.getVersion());
    afterMap.put("price", saved.getPrice());
    afterMap.put("maturityDate", saved.getMaturityDate());
    hist.setBefore(beforeMap);
    hist.setAfter(afterMap);
    hist.setActor("system");
    hist.setTimestamp(Instant.now());

    tradeHistoryRepository.save(hist);

    return saved;
  }

  // The previous updateAfterConflict fallback has been removed because the
  // repository now performs an atomic upsert (INSERT ... ON CONFLICT DO UPDATE).
  // This keeps the service simpler and avoids complex transaction/session tricks.
}
