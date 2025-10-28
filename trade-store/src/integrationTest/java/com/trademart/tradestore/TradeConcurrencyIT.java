package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

import com.trademart.tradestore.mongo.TradeHistory;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
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
@org.springframework.context.annotation.Import({
  com.trademart.tradestore.testconfig.TestJwtDecoderConfig.class,
  com.trademart.tradestore.testconfig.TestExpiryConfig.class
})
@Testcontainers
@Tag("integration")
public class TradeConcurrencyIT {

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
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    registry.add("trademart.expiry.enabled", () -> "false");
  }

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Autowired TradeRepository tradeRepository;

  @Autowired TradeHistoryRepository tradeHistoryRepository;

  @BeforeAll
  static void beforeAll() throws Exception {
    // ensure mongo answers commands before running concurrent tests
    ensureMongoResponds();
  }

  private static void ensureMongoResponds() throws Exception {
    final int maxAttempts = 60; // ~60s
    final long pauseMs = 1000L;
    for (int i = 0; i < maxAttempts; i++) {
      try {
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
        }
      } catch (Throwable t) {
      }
      Thread.sleep(pauseMs);
    }
    throw new IllegalStateException("Mongo container did not respond to ping within timeout");
  }

  @Test
  void concurrentPosts_shouldResultInHighestVersionPersisted_andHistoryWritten() throws Exception {
    final String url = "http://localhost:" + port + "/trades";
    final String tradeId = "IT-CONCUR";
    final int threads = 8;

    ExecutorService ex = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<ResponseEntity<String>>> futures = new CopyOnWriteArrayList<>();

    for (int i = 1; i <= threads; i++) {
      final int v = i;
      futures.add(
          ex.submit(
              () -> {
                ready.countDown();
                start.await();
                String body =
                    "{\n"
                        + "  \"tradeId\": \""
                        + tradeId
                        + "\",\n"
                        + "  \"version\": "
                        + v
                        + ",\n"
                        + "  \"maturityDate\": \""
                        + LocalDate.now().plusDays(5 + v)
                        + "\",\n"
                        + "  \"price\": "
                        + (100 + v)
                        + "\n"
                        + "}";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth("valid-token");
                HttpEntity<String> req = new HttpEntity<>(body, headers);

                return restTemplate.postForEntity(url, req, String.class);
              }));
    }

    // wait all threads ready, then release simultaneously
    ready.await();
    start.countDown();

    for (Future<ResponseEntity<String>> f : futures) {
      ResponseEntity<String> r = f.get(30, TimeUnit.SECONDS);
      assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
    }

    ex.shutdownNow();

    // give a small grace for DB writes
    Thread.sleep(1000);

    // With a DB-level unique constraint on tradeId we expect a single row per
    // tradeId.
    var opt = tradeRepository.findByTradeId(tradeId);
    assertThat(opt).isPresent();
    var persisted = opt.get();
    assertThat(persisted.getVersion()).isEqualTo(threads);

    List<TradeHistory> history = tradeHistoryRepository.findByTradeIdOrderByVersionDesc(tradeId);
    assertThat(history).isNotEmpty();
    // there should be at least one entry with version == threads
    boolean foundMax =
        history.stream().anyMatch(h -> Integer.valueOf(threads).equals(h.getVersion()));
    assertThat(foundMax).isTrue();
  }
}
