package com.trademart.tradestore.streaming;

import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.service.TradeService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Metrics;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
@Validated
public class TradeIngestionController {
  private final TradeService tradeService;
  private final MeterRegistry meterRegistry;
  private final Counter ingestRequests;
  private final Counter ingestErrors;
  private final Timer ingestTimer;

  public TradeIngestionController(TradeService tradeService, MeterRegistry meterRegistry) {
    this.tradeService = tradeService;
    this.meterRegistry = meterRegistry;
    this.ingestRequests = meterRegistry.counter("trade_ingest_requests_total");
    this.ingestErrors = meterRegistry.counter("trade_ingest_errors_total");
    this.ingestTimer = meterRegistry.timer("trade_ingest_latency_seconds");
  }

  @PostMapping
  public ResponseEntity<String> ingest(@Valid @RequestBody TradeDto trade) {
    ingestRequests.increment();
    var sample = Timer.start(meterRegistry);
    try {
      // persist trade and write history
      var saved = tradeService.createOrUpdateTrade(trade);
      sample.stop(ingestTimer);
      // return 201 Created with location header pointing to resource
      return ResponseEntity.created(
          org.springframework.web.util.UriComponentsBuilder.fromPath("/trades/{id}")
              .buildAndExpand(saved.getTradeId())
              .toUri())
          .body("created");
    } catch (RuntimeException ex) {
      ingestErrors.increment();
      sample.stop(ingestTimer);
      throw ex;
    }
  }
}
