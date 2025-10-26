package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

import com.trademart.tradestore.mongo.TradeHistory;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
public class TradeSequencingIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
      .withDatabaseName("test")
      .withUsername("test")
      .withPassword("test");

  @Container
  static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0.8")
      .waitingFor(Wait.forLogMessage(".*waiting for connections.*\\n", 1))
      .withStartupTimeout(Duration.ofSeconds(120));

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate restTemplate;

  @Autowired
  TradeHistoryRepository tradeHistoryRepository;

  @BeforeAll
  static void beforeAll() throws Exception {
    // ensure mongo is responsive before tests proceed (will throw if not)
    final int maxAttempts = 60;
    final long pauseMs = 1000L;
    for (int i = 0; i < maxAttempts; i++) {
      try {
        try {
          var res = mongo.execInContainer("mongosh", "--eval", "db.adminCommand({ping:1})", "--quiet");
          if (res != null && res.getExitCode() == 0)
            return;
        } catch (Throwable ignored) {
        }
        try {
          var res2 = mongo.execInContainer("mongo", "--eval", "db.adminCommand({ping:1})");
          if (res2 != null && res2.getExitCode() == 0)
            return;
        } catch (Throwable ignored) {
        }
      } catch (Throwable t) {
        // ignore and retry
      }
      Thread.sleep(pauseMs);
    }
    throw new IllegalStateException("Mongo container did not respond to ping within timeout");
  }

  @Test
  void ingestedBatch_shouldHaveMonotonicSequenceOrdering() throws Exception {
    // Submit trades out of logical order: B, A, C
    String url = "http://localhost:" + port + "/trades";

    String tb = "{"
        + "\"tradeId\": \"SEQ-B\","
        + "\"version\": 1,"
        + "\"maturityDate\": \"2030-01-01\","
        + "\"price\": 10.0"
        + "}";

    String ta = "{"
        + "\"tradeId\": \"SEQ-A\","
        + "\"version\": 1,"
        + "\"maturityDate\": \"2030-01-01\","
        + "\"price\": 20.0"
        + "}";

    String tc = "{"
        + "\"tradeId\": \"SEQ-C\","
        + "\"version\": 1,"
        + "\"maturityDate\": \"2030-01-01\","
        + "\"price\": 30.0"
        + "}";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    restTemplate.postForEntity(url, new HttpEntity<>(tb, headers), String.class);
    restTemplate.postForEntity(url, new HttpEntity<>(ta, headers), String.class);
    restTemplate.postForEntity(url, new HttpEntity<>(tc, headers), String.class);

    // Give a short pause to allow async writes (if any) to complete
    Thread.sleep(500);

    List<TradeHistory> all = tradeHistoryRepository.findAll();
    // filter to our three trades and sort by sequence asc
    List<TradeHistory> selected = all.stream()
        .filter(h -> Map.of("SEQ-A", true, "SEQ-B", true, "SEQ-C", true).containsKey(h.getTradeId()))
        .filter(h -> h.getSequence() != null)
        .sorted((a, b) -> Long.compare(a.getSequence(), b.getSequence()))
        .collect(Collectors.toList());

    assertThat(selected).hasSize(3);

    // expected ingest order was: SEQ-B, SEQ-A, SEQ-C
    assertThat(selected.get(0).getTradeId()).isEqualTo("SEQ-B");
    assertThat(selected.get(1).getTradeId()).isEqualTo("SEQ-A");
    assertThat(selected.get(2).getTradeId()).isEqualTo("SEQ-C");
  }
}
