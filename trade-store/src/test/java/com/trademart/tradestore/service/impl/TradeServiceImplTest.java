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
  private final TradeHistoryRepository tradeHistoryRepository = Mockito.mock(TradeHistoryRepository.class);
  private final com.trademart.tradestore.service.TradeSequencer tradeSequencer = Mockito
      .mock(com.trademart.tradestore.service.TradeSequencer.class);
  private final com.trademart.tradestore.service.TradeVersionValidator versionValidator = Mockito
      .mock(com.trademart.tradestore.service.TradeVersionValidator.class);
  private final com.trademart.tradestore.service.TradeMaturityValidator maturityValidator = Mockito
      .mock(com.trademart.tradestore.service.TradeMaturityValidator.class);

  private TradeServiceImpl subject;

  @BeforeEach
  void setUp() {
    // default sequencing value for tests
    when(tradeSequencer.nextSequence()).thenReturn(1L);
    // emulate original version validation logic
    org.mockito.Mockito.doAnswer(
        inv -> {
          com.trademart.tradestore.model.TradeDto incoming = inv.getArgument(0);
          com.trademart.tradestore.model.TradeEntity existing = inv.getArgument(1);
          if (existing != null
              && incoming.getVersion() != null
              && incoming.getVersion() < existing.getVersion()) {
            throw new com.trademart.tradestore.exception.TradeRejectedException(
                "incoming version is lower than existing");
          }
          return null;
        })
        .when(versionValidator)
        .validate(any(), any());

    // default maturity validator behavior (no-op) unless stubbed in tests
    org.mockito.Mockito.doNothing().when(maturityValidator).validate(any());

    subject = new TradeServiceImpl(
        tradeRepository,
        tradeHistoryRepository,
        tradeSequencer,
        versionValidator,
        maturityValidator);
  }

  @Test
  void shouldRejectLowerVersion() {
    TradeEntity existing = new TradeEntity(
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
}
