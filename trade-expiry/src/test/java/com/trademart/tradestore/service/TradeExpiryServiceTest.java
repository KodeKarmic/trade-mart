package com.trademart.tradestore.service;

import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.model.TradeStatus;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TradeExpiryServiceTest {

  @Test
  void expireDueTrades_marksExpiredAndWritesHistory() {
    TradeRepository tradeRepo = Mockito.mock(TradeRepository.class);
    TradeHistoryRepository histRepo = Mockito.mock(TradeHistoryRepository.class);
    ClockService cs =
        new ClockService(
            java.time.Clock.fixed(Instant.parse("2025-10-27T00:00:00Z"), ZoneOffset.UTC));

    TradeEntity t =
        new TradeEntity(
            "T-X",
            1,
            new BigDecimal("10.00"),
            1,
            LocalDate.parse("2025-10-26"),
            TradeStatus.ACTIVE);
    when(tradeRepo.findByStatusAndMaturityDateBefore(
            Mockito.eq(TradeStatus.ACTIVE), Mockito.any(LocalDate.class)))
        .thenReturn(List.of(t));

    TradeExpiryService svc = new TradeExpiryService(tradeRepo, histRepo, cs);
    List<TradeEntity> res = svc.expireDueTrades();

    // verify trade status changed and saved
    ArgumentCaptor<Iterable<TradeEntity>> cap = ArgumentCaptor.forClass(Iterable.class);
    verify(tradeRepo, times(1)).saveAll(cap.capture());

    Iterable<TradeEntity> saved = cap.getValue();
    TradeEntity savedFirst = saved.iterator().next();
    assert (savedFirst.getStatus() == TradeStatus.EXPIRED);

    // verify history saved
    verify(histRepo, times(1)).saveAll(anyIterable());
  }
}
