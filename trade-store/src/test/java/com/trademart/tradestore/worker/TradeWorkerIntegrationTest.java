package com.trademart.tradestore.worker;

import static org.mockito.Mockito.timeout;

import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.service.TradeService;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// no unused static imports

@Testcontainers
@SpringBootTest(
    classes = BatchWorkerApplication.class,
    properties = {
      "spring.main.web-application-type=none",
      "spring.profiles.active=batch",
      "batch.exitOnComplete=false"
    })
class TradeWorkerIntegrationTest {

  @Container
  static KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("batch.bootstrapServers", kafka::getBootstrapServers);
    registry.add("batch.topic", () -> "test-trades-topic");
    registry.add("batch.maxMessages", () -> "3");
  }

  @BeforeAll
  static void produceMessagesBeforeContext() {
    // produce messages so they exist when the BatchWorker runs during context
    // startup
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
      producer.send(
          new ProducerRecord<>(
              "test-trades-topic", "{\"tradeId\":\"T-1\",\"version\":1,\"price\":10.5}"));
      producer.send(
          new ProducerRecord<>(
              "test-trades-topic", "{\"tradeId\":\"T-2\",\"version\":1,\"price\":20.5}"));
      producer.send(
          new ProducerRecord<>(
              "test-trades-topic", "{\"tradeId\":\"T-3\",\"version\":1,\"price\":30.5}"));
      producer.flush();
    }
  }

  @MockBean TradeService tradeService;

  // context started by @SpringBootTest

  @Test
  void workerProcessesMessagesFromKafka() throws Exception {
    // messages are produced before the context starts in @BeforeAll

    // the BatchWorker CommandLineRunner runs during context initialization and will
    // process messages.
    // Verify the TradeService#createOrUpdateTrade was called 3 times within a
    // timeout.
    Mockito.verify(tradeService, timeout(10_000).times(3))
        .createOrUpdateTrade(Mockito.any(TradeDto.class));
  }
}
