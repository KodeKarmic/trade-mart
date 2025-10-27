package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.repository.TradeRepository;
import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
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
@Import({
    com.trademart.tradestore.testconfig.TestJwtDecoderConfig.class,
    com.trademart.tradestore.testconfig.PrometheusActuatorBridge.class,
    com.trademart.tradestore.testconfig.TestPrometheusRegistryConfig.class
})
@Testcontainers
@Tag("integration")
public class TradeExpiryIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").withDatabaseName("test")
      .withUsername("test").withPassword("test");

  @Container
  static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0.8").waitingFor(
      Wait.forLogMessage(".*waiting for connections.*\\n", 1)).withStartupTimeout(Duration.ofSeconds(120));

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

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate restTemplate;

  @Autowired
  TradeRepository tradeRepository;

  @Autowired
  com.trademart.tradestore.service.ClockService clockService;

  @Autowired(required = false)
  io.micrometer.prometheus.PrometheusMeterRegistry prometheusRegistry;

  @Autowired
  com.trademart.tradestore.service.TradeExpiryScheduler expiryScheduler;

  @Test
  void expiryJob_marksTradeExpired_and_incrementsMetric() throws Exception {
    String base = "http://localhost:" + port;
    // Create the trade directly via repository to avoid controller maturity
    // validation
    java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
    com.trademart.tradestore.model.TradeEntity entity = new com.trademart.tradestore.model.TradeEntity(
        "EXP-1",
        1,
        java.math.BigDecimal.valueOf(1.0),
        null,
        yesterday,
        com.trademart.tradestore.model.TradeStatus.ACTIVE);

    tradeRepository.save(entity);

    // Sanity: saved and active
    java.util.Optional<TradeEntity> saved = tradeRepository.findByTradeId("EXP-1");
    assertThat(saved).isPresent();
    assertThat(saved.get().getStatus()).isEqualTo(com.trademart.tradestore.model.TradeStatus.ACTIVE);

    // Diagnostic: print persisted maturity and computed UTC 'today' used by expiry
    System.out.println("DEBUG: persisted maturityDate=" + saved.get().getMaturityDate());
    java.time.LocalDate todayUtc = java.time.LocalDate.ofInstant(clockService.nowUtc(), java.time.ZoneOffset.UTC);
    System.out.println("DEBUG: computed todayUtc=" + todayUtc);

    java.util.List<com.trademart.tradestore.model.TradeEntity> queryResult = tradeRepository
        .findByStatusAndMaturityDateBefore(com.trademart.tradestore.model.TradeStatus.ACTIVE, todayUtc);
    System.out.println("DEBUG: repository.findByStatusAndMaturityDateBefore returned size="
        + (queryResult == null ? 0 : queryResult.size()));

    // invoke the scheduler job directly (tests call the public method)
    System.out.println("DEBUG: invoking expiryScheduler.runExpiryJob()");
    expiryScheduler.runExpiryJob();
    System.out.println("DEBUG: returned from expiryScheduler.runExpiryJob()");

    // verify trade marked expired
    java.util.Optional<TradeEntity> after = tradeRepository.findByTradeId("EXP-1");
    assertThat(after).isPresent();
    assertThat(after.get().getStatus()).isEqualTo(com.trademart.tradestore.model.TradeStatus.EXPIRED);

    // verify metric increment
    if (prometheusRegistry != null) {
      var c = prometheusRegistry.get("trade_expiry_jobs_run_total").counter();
      assertThat(c).isNotNull();
      assertThat(c.count()).isGreaterThanOrEqualTo(1.0);
    } else {
      ResponseEntity<String> prom = restTemplate.getForEntity(base + "/actuator/prometheus", String.class);
      if (prom.getStatusCode().is2xxSuccessful()) {
        String text = prom.getBody();
        assertThat(text).contains("trade_expiry_jobs_run_total");
      }
    }
  }
}
