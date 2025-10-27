package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

import com.trademart.tradestore.repository.TradeRepository;
import java.time.Duration;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.context.annotation.Import(
    com.trademart.tradestore.testconfig.TestJwtDecoderConfig.class)
@Testcontainers
@Tag("integration")
public class TradeVersionIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static org.testcontainers.containers.MongoDBContainer mongo =
      new org.testcontainers.containers.MongoDBContainer("mongo:6.0.8");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("trademart.expiry.enabled", () -> "false");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    // configure MongoDB URI for tests
    registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
  }

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Autowired TradeRepository tradeRepository;

  @BeforeAll
  static void beforeAll() throws Exception {
    // ensure container is ready (Testcontainers handles startup)
    postgres.waitingFor(Wait.forListeningPort()).withStartupTimeout(Duration.ofSeconds(60));
  }

  @Test
  void lowerVersionShouldBeRejected_andHigherVersionAccepted() {
    String baseUrl = "http://localhost:" + port + "/trades";

    // create initial trade with version 5
    String t1 =
        "{\"tradeId\": \"DB-T1\", \"version\": 5, \"maturityDate\": \"2030-01-01\", \"price\": 100.00}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth("valid-token");
    ResponseEntity<String> r1 =
        restTemplate.postForEntity(baseUrl, new HttpEntity<>(t1, headers), String.class);
    System.out.println("r1 status=" + r1.getStatusCode());
    assertThat(r1.getStatusCode().is2xxSuccessful()).isTrue();

    Integer max1 = tradeRepository.findMaxVersionByTradeId("DB-T1");
    assertThat(max1).isEqualTo(5);

    // now attempt to post lower version 4 -> expect 4xx via service validation
    // include required fields so request passes DTO validation and is rejected by
    // version logic
    String lower =
        "{\"tradeId\": \"DB-T1\", \"version\": 4, \"maturityDate\": \"2030-01-01\", \"price\": 99.00}";
    ResponseEntity<String> r2 =
        restTemplate.postForEntity(baseUrl, new HttpEntity<>(lower, headers), String.class);
    System.out.println("r2 status=" + r2.getStatusCode());
    assertThat(r2.getStatusCode().is4xxClientError()).isTrue();

    Integer maxAfterLower = tradeRepository.findMaxVersionByTradeId("DB-T1");
    // ensure the DB still shows version 5
    System.out.println("maxAfterLower=" + maxAfterLower);
    assertThat(maxAfterLower).isEqualTo(5);

    // now post higher version 6 -> should succeed and update DB
    String higher =
        "{\"tradeId\": \"DB-T1\", \"version\": 6, \"maturityDate\": \"2030-01-01\", \"price\": 110.00}";
    ResponseEntity<String> r3 =
        restTemplate.postForEntity(baseUrl, new HttpEntity<>(higher, headers), String.class);
    System.out.println("r3 status=" + r3.getStatusCode());
    assertThat(r3.getStatusCode().is2xxSuccessful()).isTrue();

    Integer maxAfterHigher = tradeRepository.findMaxVersionByTradeId("DB-T1");
    assertThat(maxAfterHigher).isEqualTo(6);
  }
}
