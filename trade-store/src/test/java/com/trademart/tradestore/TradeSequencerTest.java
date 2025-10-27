package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trademart.tradestore.service.impl.TradeSequencerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

public class TradeSequencerTest {

  @Mock private JdbcTemplate jdbcTemplate;

  private TradeSequencerImpl sequencer;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    sequencer = new TradeSequencerImpl(jdbcTemplate);
  }

  @Test
  void nextSequence_readsFromJdbcTemplate() {
    when(jdbcTemplate.queryForObject("SELECT nextval('trade_ingest_seq')", Long.class))
        .thenReturn(123L);

    long v = sequencer.nextSequence();

    assertThat(v).isEqualTo(123L);
  }
}
