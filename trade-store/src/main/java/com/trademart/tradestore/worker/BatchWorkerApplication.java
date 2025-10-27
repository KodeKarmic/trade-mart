package com.trademart.tradestore.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.service.TradeService;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

/**
 * Lightweight batch worker application. Run with `--spring.profiles.active=batch` (or the
 * Dockerfile sets this) to start the worker. It consumes up to `batch.maxMessages` messages and
 * then exits.
 */
@SpringBootApplication
@Profile("batch")
public class BatchWorkerApplication implements CommandLineRunner {

  @Value("${batch.bootstrapServers:}")
  private String bootstrapServers;

  @Value("${batch.topic:trades}")
  private String topic;

  @Value("${batch.groupId:trade-store-batch-group}")
  private String groupId;

  @Value("${batch.maxMessages:100}")
  private int maxMessages;

  @Value("${batch.exitOnComplete:true}")
  private boolean exitOnComplete;

  private final TradeService tradeService;
  private final ObjectMapper mapper = new ObjectMapper();

  @Autowired
  public BatchWorkerApplication(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  public static void main(String[] args) {
    SpringApplication.run(BatchWorkerApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    if (bootstrapServers == null || bootstrapServers.isBlank()) {
      // allow environment fallback
      bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
    }
    if (bootstrapServers == null || bootstrapServers.isBlank()) {
      System.err.println(
          "No Kafka bootstrap servers configured. Set batch.bootstrapServers or KAFKA_BOOTSTRAP_SERVERS.");
      System.exit(2);
    }

    Properties props = new Properties();
    props.put("bootstrap.servers", bootstrapServers);
    props.put("group.id", groupId);
    props.put("key.deserializer", StringDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    props.put("auto.offset.reset", "earliest");
    props.put("enable.auto.commit", "true");

    int consumed = 0;
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
      consumer.subscribe(List.of(topic));
      while (consumed < maxMessages) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
        for (ConsumerRecord<String, String> record : records) {
          try {
            TradeDto dto = mapper.readValue(record.value(), TradeDto.class);
            tradeService.createOrUpdateTrade(dto);
            consumed++;
            if (consumed >= maxMessages) break;
          } catch (Exception ex) {
            System.err.println("Worker failed to process message: " + ex.getMessage());
            // continue processing other messages
          }
        }
      }
    }

    System.out.println("Batch worker processed " + consumed + " messages.");
    // In production we may want the process to exit so a Kubernetes Job completes.
    // Tests set batch.exitOnComplete=false so the JVM is not terminated during test
    // runs.
    if (exitOnComplete) {
      System.exit(0);
    }
  }
}
