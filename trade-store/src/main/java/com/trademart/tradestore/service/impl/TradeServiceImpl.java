package com.trademart.tradestore.service.impl;

import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.model.TradeStatus;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import com.trademart.tradestore.service.TradeMaturityValidator;
import com.trademart.tradestore.service.TradeSequencer;
import com.trademart.tradestore.service.TradeService;
import com.trademart.tradestore.service.TradeVersionValidator;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradeServiceImpl implements TradeService {

  private final TradeRepository tradeRepository;
  private final TradeHistoryRepository tradeHistoryRepository;
  private final TradeSequencer tradeSequencer;
  private final TradeVersionValidator versionValidator;
  private final TradeMaturityValidator maturityValidator;

  // Removed legacy convenience constructor to decouple from relocated lifecycle
  // code.

  @Autowired
  public TradeServiceImpl(
      TradeRepository tradeRepository,
      TradeHistoryRepository tradeHistoryRepository,
      TradeSequencer tradeSequencer,
      TradeVersionValidator versionValidator,
      TradeMaturityValidator maturityValidator) {
    this.tradeRepository = tradeRepository;
    this.tradeHistoryRepository = tradeHistoryRepository;
    this.tradeSequencer = tradeSequencer;
    this.versionValidator = versionValidator;
    this.maturityValidator = maturityValidator;
  }

  @Override
  public TradeEntity createOrUpdateTrade(TradeDto dto) {
    // Simple version validation and upsert behavior
    var existingOpt = tradeRepository.findByTradeId(dto.getTradeId());

    TradeEntity before = null;
    if (existingOpt.isPresent()) {
      before = existingOpt.get();
    }

    // delegate version validation to the validator component
    versionValidator.validate(dto, before);

    // delegate maturity validation (pass only the maturity date after refactor)
    maturityValidator.validate(dto.getMaturityDate());

    TradeEntity entity = existingOpt.orElseGet(() -> new TradeEntity());
    // map fields
    entity.setTradeId(dto.getTradeId());
    entity.setVersion(dto.getVersion());
    entity.setPrice(dto.getPrice());
    entity.setMaturityDate(dto.getMaturityDate());
    entity.setStatus(TradeStatus.ACTIVE);
    entity.setUpdatedAt(Instant.now());
    if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());

    // assign ingest sequence for ordering (must be done before persisting the row)
    long seq = tradeSequencer.nextSequence();
    entity.setIngestSequence(seq);

    // Use an atomic DB upsert to avoid concurrent-insert races.
    TradeEntity saved =
        tradeRepository.upsertTrade(
            entity.getTradeId(),
            entity.getVersion(),
            entity.getPrice(),
            entity.getQuantity(),
            entity.getMaturityDate(),
            entity.getIngestSequence(),
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
    // include the same sequence in history
    hist.setSequence(entity.getIngestSequence());
    afterMap.put("sequence", entity.getIngestSequence());

    tradeHistoryRepository.save(hist);

    return saved;
  }

  // The previous updateAfterConflict fallback has been removed because the
  // repository now performs an atomic upsert (INSERT ... ON CONFLICT DO UPDATE).
  // This keeps the service simpler and avoids complex transaction/session tricks.
}
