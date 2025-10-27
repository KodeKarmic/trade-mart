package com.trademart.tradestore.streaming;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Simple in-memory counter of in-flight/pending messages being processed by this instance. A
 * Micrometer Gauge can be bound to this value so an autoscaler can make decisions.
 */
@Component
public class TradeProcessingState {
  private final AtomicLong pending = new AtomicLong(0);

  public void increment() {
    pending.incrementAndGet();
  }

  public void decrement() {
    pending.updateAndGet(v -> v > 0 ? v - 1 : 0);
  }

  public long getPending() {
    return pending.get();
  }
}
