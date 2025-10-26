package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

import com.trademart.tradestore.mongo.TradeHistory;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.http.ResponseEntity;
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
public class TradeIngestionIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static MongoDBContainer mongo =
      new MongoDBContainer("mongo:6.0.8")
          // use a log-based wait strategy and extend the startup timeout to be more
          // tolerant of transient socket/read issues during Mongo replica set
          // initialization
          .waitingFor(Wait.forLogMessage(".*waiting for connections.*\\n", 1))
          .withStartupTimeout(Duration.ofSeconds(120));

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    // Ensure Hibernate will create the schema for the integration database instance
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
  }

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Autowired TradeRepository tradeRepository;

  @Autowired TradeHistoryRepository tradeHistoryRepository;

  @BeforeAll
  static void beforeAll() throws Exception {
    // containers are started by Testcontainers JUnit integration
    ensureMongoResponds();
  }

  private static void ensureMongoResponds() throws Exception {
    final int maxAttempts = 60; // ~60s
    final long pauseMs = 1000L;
    for (int i = 0; i < maxAttempts; i++) {
      try {
        // prefer mongosh (newer images); fall back to legacy 'mongo' if present
        try {
          var res =
              mongo.execInContainer("mongosh", "--eval", "db.adminCommand({ping:1})", "--quiet");
          if (res != null && res.getExitCode() == 0) {
            String out = res.getStdout();
            if (out != null && out.toLowerCase().contains("ok")) {
              return;
            }
          }
        } catch (Throwable ignored) {
          // try legacy client
        }

        try {
          var res2 = mongo.execInContainer("mongo", "--eval", "db.adminCommand({ping:1})");
          if (res2 != null && res2.getExitCode() == 0) {
            String out = res2.getStdout();
            if (out != null && out.toLowerCase().contains("ok")) {
              return;
            }
          }
        } catch (Throwable ignored) {
          // swallow and retry
        }
      } catch (Throwable t) {
        // ignore and retry
      }
      Thread.sleep(pauseMs);
    }
    throw new IllegalStateException("Mongo container did not respond to ping within timeout");
  }

  @AfterAll
  static void afterAll() {
    // containers will be stopped automatically
  }

  @Test
  void invalidTradeShouldBeRejected() {
    String url = "http://localhost:" + port + "/trades";
    String body = "{\"tradeId\": \"IT-INVALID\", \"version\": -1}"; // missing required fields

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> req = new HttpEntity<>(body, headers);

    ResponseEntity<String> resp = restTemplate.postForEntity(url, req, String.class);
    assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
  }

  @Test
  void validTradeShouldBePersistedAndReturn201() {
    String url = "http://localhost:" + port + "/trades";
    String body =
        "{"
            + "\"tradeId\": \"IT-T1\","
            + "\"version\": 1,"
            + "\"maturityDate\": \"2030-01-01\","
            + "\"price\": 123.45"
            + "}";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> req = new HttpEntity<>(body, headers);

    ResponseEntity<String> resp = restTemplate.postForEntity(url, req, String.class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

    // verify trade stored in Postgres
    var opt = tradeRepository.findByTradeId("IT-T1");
    assertThat(opt).isPresent();

    // verify history written to Mongo
    List<TradeHistory> history = tradeHistoryRepository.findByTradeIdOrderByVersionDesc("IT-T1");
    assertThat(history).isNotEmpty();

    TradeHistory hist = history.get(0);
    assertThat(hist.getChangeType()).isEqualTo("CREATE");
    // For a create the 'before' map should be empty and 'after' should contain the
    // persisted fields
    assertThat(hist.getBefore()).isEmpty();
    assertThat(hist.getAfter()).containsEntry("tradeId", "IT-T1");
    assertThat(hist.getAfter()).containsEntry("version", 1);
    // maturityDate may be stored as LocalDate, java.util.Date (timestamp) or String
    // depending on Mongo mapping.
    Object mdObj = hist.getAfter().get("maturityDate");
    LocalDate mdValue;
    if (mdObj instanceof LocalDate) {
      mdValue = (LocalDate) mdObj;
    } else if (mdObj instanceof java.util.Date) {
      mdValue =
          ((java.util.Date) mdObj)
              .toInstant()
              .atZone(java.time.ZoneId.systemDefault())
              .toLocalDate();
    } else {
      mdValue = LocalDate.parse(mdObj.toString());
    }
    assertThat(mdValue).isEqualTo(LocalDate.parse("2030-01-01"));

    // price may be represented as BigDecimal or numeric string; compare numerically
    Object priceObj = hist.getAfter().get("price");
    BigDecimal priceValue = new BigDecimal(priceObj.toString());
    assertThat(priceValue).isEqualByComparingTo(new BigDecimal("123.45"));
  }
}
