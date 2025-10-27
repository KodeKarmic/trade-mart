package com.trademart.tradestore.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Conditional scheduled invoker that calls the core TradeExpiryScheduler.
 * The invoker bean is only created when the property trademart.expiry.enabled
 * is true (or missing). Tests can set the property to false to avoid the
 * automatic scheduled execution while still autowiring the core scheduler.
 */
@Component
@ConditionalOnProperty(name = "trademart.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class TradeExpiryAutoScheduler {

  private final TradeExpiryScheduler scheduler;

  public TradeExpiryAutoScheduler(TradeExpiryScheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Scheduled(fixedRateString = "${trademart.expiry.frequency:60000}", initialDelayString = "${trademart.expiry.initialDelay:10000}")
  public void scheduledRun() {
    scheduler.runExpiryJob();
  }
}
