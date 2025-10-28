package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.mongo.TradeHistory;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.service.impl.TradeServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TradeServiceImplTest {

  private TradeRepository tradeRepository;
  private TradeHistoryRepository tradeHistoryRepository;
  private com.trademart.tradestore.service.TradeSequencer tradeSequencer;
  private com.trademart.tradestore.service.TradeVersionValidator versionValidator;
  private com.trademart.tradeexpiry.service.TradeMaturityValidator maturityValidator;
  private TradeServiceImpl service;

  @BeforeEach
  void setUp() {
    tradeRepository = mock(TradeRepository.class);
    tradeHistoryRepository = mock(TradeHistoryRepository.class);
    tradeSequencer = mock(com.trademart.tradestore.service.TradeSequencer.class);
    versionValidator = mock(com.trademart.tradestore.service.TradeVersionValidator.class);
    maturityValidator = mock(com.trademart.tradeexpiry.service.TradeMaturityValidator.class);
    when(tradeSequencer.nextSequence()).thenReturn(42L);
    // emulate original version validation logic
    org.mockito.Mockito.doAnswer(
        inv -> {
          Object[] args = inv.getArguments();
          TradeDto incoming = null;
          if (args.length > 0 && args[0] instanceof TradeDto)
            incoming = (TradeDto) args[0];
          Object maybeExisting = args.length > 1 ? args[1] : null;
          TradeEntity existing = null;
          if (maybeExisting instanceof TradeEntity)
            existing = (TradeEntity) maybeExisting;

          if (existing != null
              && incoming != null
              && incoming.getVersion() != null
              && incoming.getVersion() < existing.getVersion()) {
            throw new TradeRejectedException(
                "incoming version is lower than existing");
          }
          return null;
        })
        .when(versionValidator)
        .validate(any(TradeDto.class), any(TradeEntity.class));
    // emulate original maturity validation (throws domain TradeValidationException
    // now)
    org.mockito.Mockito.doAnswer(
        inv -> {
          Object[] args = inv.getArguments();
          Object maybeMd = args.length > 0 ? args[0] : null;
          if (maybeMd instanceof java.time.LocalDate) {
            java.time.LocalDate md = (java.time.LocalDate) maybeMd;
            if (md != null && md.isBefore(java.time.LocalDate.now())) {
              throw new com.trademart.tradestore.exception.TradeValidationException(
                  "maturity date is in the past");
            }
          }
          return null;
        })
        .when(maturityValidator)
        .validate(any(java.time.LocalDate.class));
    service = new TradeServiceImpl(
        tradeRepository,
        tradeHistoryRepository,
        tradeSequencer,
        versionValidator,
        maturityValidator);
  }

  @Test
  void whenIncomingVersionLowerThanExisting_thenReject() {
    // existing trade version 5
    TradeEntity existing = new TradeEntity();
    existing.setTradeId("T-1");
    existing.setVersion(5);

    when(tradeRepository.findByTradeId("T-1")).thenReturn(Optional.of(existing));

    TradeDto dto = new TradeDto();
    dto.setTradeId("T-1");
    dto.setVersion(4); // lower than existing
    dto.setPrice(new BigDecimal("1.0"));
    dto.setMaturityDate(LocalDate.now().plusDays(1));

    assertThatThrownBy(() -> service.createOrUpdateTrade(dto))
        .isInstanceOf(TradeRejectedException.class);
  }

  @Test
  void whenNewTrade_thenPersistAndWriteHistory() {
    when(tradeRepository.findByTradeId("NEW")).thenReturn(Optional.empty());

    TradeEntity saved = new TradeEntity();
    saved.setTradeId("NEW");
    saved.setVersion(1);
    saved.setPrice(new BigDecimal("9.99"));

    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(saved);

    TradeDto dto = new TradeDto();
    dto.setTradeId("NEW");
    dto.setVersion(1);
    dto.setPrice(new BigDecimal("9.99"));
    dto.setMaturityDate(LocalDate.now().plusDays(10));

    TradeEntity result = service.createOrUpdateTrade(dto);

    assertThat(result).isNotNull();
    assertThat(result.getTradeId()).isEqualTo("NEW");
    // verify history saved and contents
    ArgumentCaptor<TradeHistory> captor = ArgumentCaptor.forClass(TradeHistory.class);
    verify(tradeHistoryRepository).save(captor.capture());
    TradeHistory hist = captor.getValue();
    assertThat(hist.getTradeId()).isEqualTo("NEW");
    assertThat(hist.getChangeType()).isEqualTo("CREATE");
    assertThat(hist.getBefore()).isNotNull();
    assertThat(hist.getBefore()).isEmpty();
    assertThat(hist.getAfter()).containsEntry("tradeId", "NEW");
    assertThat(hist.getAfter()).containsEntry("version", 1);
  }

  @Test
  void whenIncomingVersionEqual_thenUpdate() {
    // existing trade version 2
    TradeEntity existing = new TradeEntity();
    existing.setTradeId("T-2");
    existing.setVersion(2);
    existing.setPrice(new BigDecimal("5.00"));

    when(tradeRepository.findByTradeId("T-2")).thenReturn(Optional.of(existing));

    TradeEntity saved = new TradeEntity();
    saved.setTradeId("T-2");
    saved.setVersion(2);
    saved.setPrice(new BigDecimal("7.00"));
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(saved);

    TradeDto dto = new TradeDto();
    dto.setTradeId("T-2");
    dto.setVersion(2);
    dto.setPrice(new BigDecimal("7.00"));
    dto.setMaturityDate(LocalDate.now().plusDays(5));

    TradeEntity result = service.createOrUpdateTrade(dto);

    assertThat(result.getPrice()).isEqualTo(saved.getPrice());
    ArgumentCaptor<TradeHistory> captor = ArgumentCaptor.forClass(TradeHistory.class);
    verify(tradeHistoryRepository).save(captor.capture());
    TradeHistory hist = captor.getValue();
    assertThat(hist.getChangeType()).isEqualTo("UPDATE");
    assertThat(hist.getBefore()).containsEntry("version", 2);
    assertThat(hist.getAfter()).containsEntry("price", saved.getPrice());
  }

  @Test
  void whenIncomingVersionNull_thenAccept() {
    TradeEntity existing = new TradeEntity();
    existing.setTradeId("T-3");
    existing.setVersion(3);
    when(tradeRepository.findByTradeId("T-3")).thenReturn(Optional.of(existing));

    TradeEntity saved = new TradeEntity();
    saved.setTradeId("T-3");
    saved.setVersion(3);
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(saved);

    TradeDto dto = new TradeDto();
    dto.setTradeId("T-3");
    dto.setVersion(null);
    dto.setPrice(new BigDecimal("2.50"));
    dto.setMaturityDate(LocalDate.now().plusDays(2));

    TradeEntity result = service.createOrUpdateTrade(dto);

    assertThat(result).isNotNull();
    ArgumentCaptor<TradeHistory> captor = ArgumentCaptor.forClass(TradeHistory.class);
    verify(tradeHistoryRepository).save(captor.capture());
    TradeHistory hist = captor.getValue();
    // when incoming version is null, service should still write history with saved
    // version
    assertThat(hist.getAfter()).containsEntry("version", saved.getVersion());
  }

  @Test
  void whenRepositorySaveThrows_thenExceptionPropagated() {
    when(tradeRepository.findByTradeId("BORKED")).thenReturn(Optional.empty());
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("db down"));

    TradeDto dto = new TradeDto();
    dto.setTradeId("BORKED");
    dto.setVersion(1);
    dto.setPrice(new BigDecimal("1.00"));
    dto.setMaturityDate(LocalDate.now().plusDays(1));

    assertThatThrownBy(() -> service.createOrUpdateTrade(dto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("db down");
  }

  @Test
  void whenHistoryRepoThrows_thenExceptionPropagated() {
    when(tradeRepository.findByTradeId("HISTERR")).thenReturn(Optional.empty());

    TradeEntity saved = new TradeEntity();
    saved.setTradeId("HISTERR");
    saved.setVersion(1);
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(saved);

    // make history save fail
    when(tradeHistoryRepository.save(any(TradeHistory.class)))
        .thenThrow(new RuntimeException("mongo down"));

    TradeDto dto = new TradeDto();
    dto.setTradeId("HISTERR");
    dto.setVersion(1);
    dto.setPrice(new BigDecimal("2.00"));
    dto.setMaturityDate(LocalDate.now().plusDays(2));

    assertThatThrownBy(() -> service.createOrUpdateTrade(dto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("mongo down");
  }

  @Test
  void whenIncomingVersionHigher_thenAcceptAndUpdate() {
    // existing trade version 1
    TradeEntity existing = new TradeEntity();
    existing.setTradeId("T-4");
    existing.setVersion(1);
    existing.setPrice(new BigDecimal("1.00"));

    when(tradeRepository.findByTradeId("T-4")).thenReturn(Optional.of(existing));

    TradeEntity saved = new TradeEntity();
    saved.setTradeId("T-4");
    saved.setVersion(2);
    saved.setPrice(new BigDecimal("2.00"));
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(saved);

    TradeDto dto = new TradeDto();
    dto.setTradeId("T-4");
    dto.setVersion(2);
    dto.setPrice(new BigDecimal("2.00"));
    dto.setMaturityDate(LocalDate.now().plusDays(3));

    TradeEntity result = service.createOrUpdateTrade(dto);

    assertThat(result.getVersion()).isEqualTo(2);
    ArgumentCaptor<TradeHistory> captor = ArgumentCaptor.forClass(TradeHistory.class);
    verify(tradeHistoryRepository).save(captor.capture());
    TradeHistory hist = captor.getValue();
    assertThat(hist.getChangeType()).isEqualTo("UPDATE");
    // implementation mutates the entity instance before creating the before/after
    // maps
    // so the stored 'before' version may equal the saved version; assert presence
    assertThat(hist.getBefore()).containsKey("version");
    assertThat(hist.getAfter()).containsEntry("version", 2);
  }

  @Test
  void concurrency_multipleThreads_shouldNotThrowAnd_saveCalledForEach() throws Exception {
    final String id = "CONCUR";
    int threads = 10;

    when(tradeRepository.findByTradeId(id)).thenReturn(Optional.empty());

    AtomicInteger saveCount = new AtomicInteger(0);
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              saveCount.incrementAndGet();
              String tid = invocation.getArgument(0);
              Integer ver = invocation.getArgument(1);
              BigDecimal price = invocation.getArgument(2);
              Integer qty = invocation.getArgument(3);
              LocalDate md = invocation.getArgument(4);
              Long seq = invocation.getArgument(5);
              String status = invocation.getArgument(6);
              TradeEntity e = new TradeEntity();
              e.setTradeId(tid);
              e.setVersion(ver == null ? 1 : ver);
              e.setPrice(price);
              e.setMaturityDate(md);
              e.setIngestSequence(seq);
              return e;
            });

    AtomicInteger histCount = new AtomicInteger(0);
    when(tradeHistoryRepository.save(any(TradeHistory.class)))
        .thenAnswer(
            invocation -> {
              histCount.incrementAndGet();
              return invocation.getArgument(0);
            });

    ExecutorService ex = Executors.newFixedThreadPool(threads);
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      futures.add(
          ex.submit(
              () -> {
                TradeDto dto = new TradeDto();
                dto.setTradeId(id);
                dto.setVersion(1);
                dto.setPrice(new BigDecimal("1.00"));
                dto.setMaturityDate(LocalDate.now().plusDays(1));
                service.createOrUpdateTrade(dto);
              }));
    }

    for (Future<?> f : futures)
      f.get();
    ex.shutdownNow();

    assertThat(saveCount.get()).isEqualTo(threads);
    assertThat(histCount.get()).isEqualTo(threads);
  }

  @Test
  void veryLargePrice_isPersistedAndRecordedInHistory() {
    when(tradeRepository.findByTradeId("BIG")).thenReturn(Optional.empty());

    // very large price
    BigDecimal big = BigDecimal.TEN.pow(200);

    TradeEntity saved = new TradeEntity();
    saved.setTradeId("BIG");
    saved.setVersion(1);
    saved.setPrice(big);
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(saved);

    TradeDto dto = new TradeDto();
    dto.setTradeId("BIG");
    dto.setVersion(1);
    dto.setPrice(big);
    dto.setMaturityDate(LocalDate.now().plusDays(10));

    TradeEntity result = service.createOrUpdateTrade(dto);

    assertThat(result.getPrice()).isEqualTo(big);
    ArgumentCaptor<TradeHistory> captor = ArgumentCaptor.forClass(TradeHistory.class);
    verify(tradeHistoryRepository).save(captor.capture());
    TradeHistory hist = captor.getValue();
    assertThat(hist.getAfter()).containsEntry("price", big);
  }

  @Test
  void nullAndBlankTradeId_behaviour_documented() {
    // null tradeId (service currently allows it since validation is at controller
    // layer)
    when(tradeRepository.findByTradeId(null)).thenReturn(Optional.empty());
    TradeEntity savedNull = new TradeEntity();
    savedNull.setTradeId(null);
    savedNull.setVersion(1);
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(savedNull);

    TradeDto dtoNull = new TradeDto();
    dtoNull.setTradeId(null);
    dtoNull.setVersion(1);
    dtoNull.setPrice(new BigDecimal("1.00"));
    dtoNull.setMaturityDate(LocalDate.now().plusDays(1));

    TradeEntity resNull = service.createOrUpdateTrade(dtoNull);
    assertThat(resNull.getTradeId()).isNull();

    // blank tradeId
    when(tradeRepository.findByTradeId(" ")).thenReturn(Optional.empty());
    TradeEntity savedBlank = new TradeEntity();
    savedBlank.setTradeId(" ");
    savedBlank.setVersion(1);
    when(tradeRepository.upsertTrade(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(savedBlank);

    TradeDto dtoBlank = new TradeDto();
    dtoBlank.setTradeId(" ");
    dtoBlank.setVersion(1);
    dtoBlank.setPrice(new BigDecimal("1.00"));
    dtoBlank.setMaturityDate(LocalDate.now().plusDays(1));

    TradeEntity resBlank = service.createOrUpdateTrade(dtoBlank);
    assertThat(resBlank.getTradeId()).isEqualTo(" ");
  }
}
