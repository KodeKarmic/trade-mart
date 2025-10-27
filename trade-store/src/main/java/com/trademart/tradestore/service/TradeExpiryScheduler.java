package com.trademart.tradestore.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Runs expiry job on a fixed schedule and records a metric for runs.
 */
@Component
public class TradeExpiryScheduler {

  private static final Logger log = LoggerFactory.getLogger(TradeExpiryScheduler.class);

  private final TradeExpiryService expiryService;
  private final Counter runCounter;

  public TradeExpiryScheduler(TradeExpiryService expiryService, MeterRegistry registry) {
    this.expiryService = expiryService;
    this.runCounter = Counter.builder("trade_expiry_jobs_run_total")
        .description("Number of times the trade expiry job has run")
        .register(registry);
  }

  /** Public hook so tests can invoke the job directly. */
  public void runExpiryJob() {
    try {
      // extra stdout to make test-run debugging easier (visible even when logging is
      // misconfigured)
      System.out.println("DEBUG: starting trade expiry job");
      log.info("starting trade expiry job");
      runCounter.increment();
      expiryService.expireDueTrades();
    } catch (Exception e) {
      log.error("trade expiry job failed", e);
    }
  }
}
