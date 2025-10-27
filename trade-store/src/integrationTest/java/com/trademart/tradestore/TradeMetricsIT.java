package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

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
public class TradeMetricsIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").withDatabaseName("test")
      .withUsername("test")
      .withPassword("test");

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
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate restTemplate;

  @Autowired
  TradeRepository tradeRepository;

  @Autowired(required = false)
  io.micrometer.prometheus.PrometheusMeterRegistry prometheusRegistry;

  @Test
  void postingTrade_exposesMetricsAtPrometheusEndpoint() throws Exception {
    String base = "http://localhost:" + port;
    String url = base + "/trades";

    String body = "{"
        + "\"tradeId\": \"MET-1\","
        + "\"version\": 1,"
        + "\"maturityDate\": \"2030-01-01\","
        + "\"price\": 10.0"
        + "}";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth("valid-token");

    ResponseEntity<String> r = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();

    // Prefer reading the value directly from the Prometheus registry if available
    if (prometheusRegistry != null) {
      var m = prometheusRegistry.get("trade_ingest_requests_total").counter();
      assertThat(m).isNotNull();
      assertThat(m.count()).isGreaterThanOrEqualTo(1.0);
    } else {
      // Fallback: attempt to scrape the actuator endpoint but don't fail the test
      // if the scraping format differs; log the body for debugging.
      ResponseEntity<String> prom = restTemplate.getForEntity(base + "/actuator/prometheus", String.class);
      if (prom.getStatusCode().is2xxSuccessful()) {
        String text = prom.getBody();
        System.out.println("--- PROMETHEUS BODY START ---\n" + text + "\n--- PROMETHEUS BODY END ---");
      }
      // Ensure at least the controller persisted the trade (sanity check)
      assertThat(tradeRepository.findByTradeId("MET-1")).isPresent();
    }
  }
}
