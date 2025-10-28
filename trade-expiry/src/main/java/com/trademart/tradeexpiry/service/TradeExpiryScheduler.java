package com.trademart.tradeexpiry.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TradeExpiryScheduler {
  private static final Logger log = LoggerFactory.getLogger(TradeExpiryScheduler.class);
  private final TradeExpiryService expiryService;
  private final Counter runCounter;

  public TradeExpiryScheduler(TradeExpiryService expiryService, MeterRegistry registry) {
    this.expiryService = expiryService;
    this.runCounter = Counter.builder("trade_expiry_jobs_run_total").description("Number of times the trade expiry job has run").register(registry);
  }

  @Scheduled(fixedDelayString = "${trade.expiry.fixedDelay:60000}")
  public void runExpiryJob() {
    try {
      log.info("starting trade expiry job");
      runCounter.increment();
      expiryService.expireDueTrades();
    } catch (Exception e) {
      log.error("trade expiry job failed", e);
    }
  }
}
