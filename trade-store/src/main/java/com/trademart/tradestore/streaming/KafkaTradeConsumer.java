package com.trademart.tradestore.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.service.TradeService;
import io.micrometer.observation.annotation.Observed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that reads trade messages (JSON) and persists them via TradeService. It
 * increments/decrements an in-memory counter so we can expose the backlog to an autoscaler.
 */
@Component
public class KafkaTradeConsumer {

  private static final Log LOG = LogFactory.getLog(KafkaTradeConsumer.class);

  private final ObjectMapper mapper = new ObjectMapper();
  private final TradeService tradeService;
  private final TradeProcessingState state;

  @Autowired
  public KafkaTradeConsumer(TradeService tradeService, TradeProcessingState state) {
    this.tradeService = tradeService;
    this.state = state;
  }

  @KafkaListener(
      topics = "${kafka.topic.trades:trades}",
      groupId = "${kafka.consumer.group-id:trade-store-group}")
  @Observed(name = "trade.consumer.process")
  public void consume(String message) {
    state.increment();
    try {
      TradeDto dto = mapper.readValue(message, TradeDto.class);
      tradeService.createOrUpdateTrade(dto);
    } catch (Exception ex) {
      // Swallow exceptions here and let the container/consumer handle retries based
      // on config.
      LOG.error("Failed to process trade message: " + ex.getMessage(), ex);
    } finally {
      state.decrement();
    }
  }
}
