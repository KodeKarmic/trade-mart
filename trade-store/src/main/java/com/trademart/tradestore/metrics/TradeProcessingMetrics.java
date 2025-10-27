package com.trademart.tradestore.metrics;

import com.trademart.tradestore.streaming.TradeProcessingState;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * Binds an application-level gauge exposing the number of in-flight/pending
 * trade messages.
 * This metric can be consumed by an autoscaler (KEDA or a Custom Metrics HPA)
 * to scale replicas
 * up when there is backlog and down when backlog is 0.
 */
@Component
public class TradeProcessingMetrics implements MeterBinder {

  private final TradeProcessingState state;

  public TradeProcessingMetrics(TradeProcessingState state) {
    this.state = state;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder("trade_processor_pending_messages", state, TradeProcessingState::getPending)
        .description("Number of trade messages currently pending processing in this instance")
        .register(registry);
  }
}
