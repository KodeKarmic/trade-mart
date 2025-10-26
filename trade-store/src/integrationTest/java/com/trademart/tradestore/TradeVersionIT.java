package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.mongo.TradeHistory;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public class TradeVersionIT {

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
  TradeRepository tradeRepository;

  @Autowired
  TradeHistoryRepository tradeHistoryRepository;

  @BeforeAll
  static void beforeAll() throws Exception {
    // ensure mongo is responsive before tests proceed
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
  void create_then_reject_lower_version_then_accept_higher_version() throws Exception {
    String url = "http://localhost:" + port + "/trades";

    String initial = "{"
        + "\"tradeId\": \"VT-1\","
        + "\"version\": 5,"
        + "\"maturityDate\": \"2030-01-01\","
        + "\"price\": 100.0"
        + "}";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    ResponseEntity<String> r1 = restTemplate.postForEntity(url, new HttpEntity<>(initial, headers), String.class);
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // persisted version should be 5 and one history entry
    TradeEntity te = tradeRepository.findByTradeId("VT-1").orElseThrow();
    assertThat(te.getVersion()).isEqualTo(5);
    List<TradeHistory> h1 = tradeHistoryRepository.findAll().stream()
        .filter(h -> "VT-1".equals(h.getTradeId())).collect(Collectors.toList());
    assertThat(h1).hasSize(1);

    // Now submit a lower version -> should be rejected (400)
    String lower = "{"
        + "\"tradeId\": \"VT-1\","
        + "\"version\": 4,"
        + "\"maturityDate\": \"2030-01-01\","
        + "\"price\": 90.0"
        + "}";

    ResponseEntity<String> r2 = restTemplate.postForEntity(url, new HttpEntity<>(lower, headers), String.class);
    assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // assert structured error body contains a version-specific errorCode
    // some HTTP clients may not expose the response body as String for error
    // statuses,
    // so request as a Map to ensure we can inspect the structured JSON payload.
    ResponseEntity<java.util.Map> r2map = restTemplate.postForEntity(url, new HttpEntity<>(lower, headers),
        java.util.Map.class);
    assertThat(r2map.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var bodyMap = r2map.getBody();
    assertThat(bodyMap).isNotNull();
    // if errorCode is missing, dump body for debugging
    if (bodyMap.get("errorCode") == null) {
      System.out.println("DEBUG: error response body for lower-version rejection: " + bodyMap);
    }
    assertThat(bodyMap.get("errorCode")).isEqualTo("VERSION_TOO_LOW");
    assertThat(bodyMap.get("message")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.STRING)
        .contains("incoming version is lower");

    // ensure still only one history entry and version unchanged
    TradeEntity te2 = tradeRepository.findByTradeId("VT-1").orElseThrow();
    assertThat(te2.getVersion()).isEqualTo(5);
    List<TradeHistory> h2 = tradeHistoryRepository.findAll().stream()
        .filter(h -> "VT-1".equals(h.getTradeId())).collect(Collectors.toList());
    assertThat(h2).hasSize(1);

    // Now submit a higher version -> accepted
    String higher = "{"
        + "\"tradeId\": \"VT-1\","
        + "\"version\": 6,"
        + "\"maturityDate\": \"2030-01-01\","
        + "\"price\": 110.0"
        + "}";

    ResponseEntity<String> r3 = restTemplate.postForEntity(url, new HttpEntity<>(higher, headers), String.class);
    assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    TradeEntity te3 = tradeRepository.findByTradeId("VT-1").orElseThrow();
    assertThat(te3.getVersion()).isEqualTo(6);
    List<TradeHistory> h3 = tradeHistoryRepository.findAll().stream()
        .filter(h -> "VT-1".equals(h.getTradeId())).collect(Collectors.toList());
    // create + update -> 2 history entries
    assertThat(h3).hasSize(2);
  }

  @Test
  void maturityDateBeforeToday_rejected_by_controller_with_structured_error() throws Exception {
    String url = "http://localhost:" + port + "/trades";

    String payload = "{"
        + "\"tradeId\": \"VT-MD\","
        + "\"version\": 1,"
        + "\"maturityDate\": \"2000-01-01\","
        + "\"price\": 10.0"
        + "}";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    String body = resp.getBody();
    ObjectMapper m = new ObjectMapper();
    JsonNode node = m.readTree(body);

    assertThat(node.get("status").asInt()).isEqualTo(400);
    assertThat(node.get("error").asText()).isEqualTo("Bad Request");
    assertThat(node.get("path").asText()).isEqualTo("/trades");
    assertThat(node.get("message").asText()).isEqualTo("maturity date is in the past");
    assertThat(node.has("traceId")).isTrue();
    assertThat(node.get("traceId").asText()).isNotBlank();

    String ts = node.get("timestamp").asText();
    Pattern p = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|[+-]\\d{2}:\\d{2})$");
    assertThat(p.matcher(ts).matches()).isTrue();
    // also ensure it parses as OffsetDateTime
    OffsetDateTime.parse(ts);
  }
}
