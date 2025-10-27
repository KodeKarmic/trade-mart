package com.trademart.tradestore.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.model.TradeStatus;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TradeServiceImplTest {

  private final TradeRepository tradeRepository = Mockito.mock(TradeRepository.class);
  private final TradeHistoryRepository tradeHistoryRepository =
      Mockito.mock(TradeHistoryRepository.class);
  private final com.trademart.tradestore.service.TradeSequencer tradeSequencer =
      Mockito.mock(com.trademart.tradestore.service.TradeSequencer.class);

  private final TradeServiceImpl subject =
      new TradeServiceImpl(tradeRepository, tradeHistoryRepository, tradeSequencer);

  @BeforeEach
  void setUp() {
    // default sequencing value for tests
    when(tradeSequencer.nextSequence()).thenReturn(1L);
  }

  @Test
  void shouldRejectLowerVersion() {
    TradeEntity existing =
        new TradeEntity(
            "T1",
            5,
            new BigDecimal("100.00"),
            1,
            LocalDate.parse("2025-12-31"),
            TradeStatus.ACTIVE);

    when(tradeRepository.findByTradeId("T1")).thenReturn(Optional.of(existing));

    TradeDto dto = new TradeDto();
    dto.setTradeId("T1");
    dto.setVersion(4);
    dto.setPrice(new BigDecimal("200.00"));
    dto.setMaturityDate(LocalDate.parse("2025-12-31"));

    assertThrows(TradeRejectedException.class, () -> subject.createOrUpdateTrade(dto));

    // ensure no DB upsert or history writes were attempted for rejected trades
    verify(tradeRepository, never()).upsertTrade(any(), any(), any(), any(), any(), any(), any());
    verifyNoInteractions(tradeHistoryRepository);
  }

  @Test
  void shouldRejectPastMaturityDate() {
    when(tradeRepository.findByTradeId(any())).thenReturn(Optional.empty());

    TradeDto dto = new TradeDto();
    dto.setTradeId("T2");
    dto.setVersion(1);
    dto.setPrice(new BigDecimal("100.00"));
    dto.setMaturityDate(LocalDate.now().minusDays(1));

    assertThrows(TradeRejectedException.class, () -> subject.createOrUpdateTrade(dto));

    verify(tradeRepository, never()).upsertTrade(any(), any(), any(), any(), any(), any(), any());
    verifyNoInteractions(tradeHistoryRepository);
  }
}
