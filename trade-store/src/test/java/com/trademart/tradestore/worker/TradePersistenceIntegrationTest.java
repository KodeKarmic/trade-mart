package com.trademart.tradestore.worker;

import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.mongo.TradeHistory;
import com.trademart.tradestore.repository.TradeRepository;
import com.trademart.tradestore.repository.mongo.TradeHistoryRepository;
import com.trademart.tradestore.service.TradeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class TradePersistenceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

  @Container static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0.8");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
    registry.add("spring.datasource.username", () -> postgres.getUsername());
    registry.add("spring.datasource.password", () -> postgres.getPassword());
    registry.add("spring.data.mongodb.uri", () -> mongo.getReplicaSetUrl());
    // Ensure Hibernate creates the schema for the test
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    // Re-enable Flyway for this integration test (tests resources disable it by
    // default)
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired TradeService tradeService;
  @Autowired TradeRepository tradeRepository;
  @Autowired TradeHistoryRepository tradeHistoryRepository;

  @Test
  void createTradePersistsEntityAndHistory() {
    TradeDto dto = new TradeDto();
    dto.setTradeId("P-100");
    dto.setVersion(1);
    dto.setMaturityDate(LocalDate.now().plusDays(10));
    dto.setPrice(BigDecimal.valueOf(99.99));

    var saved = tradeService.createOrUpdateTrade(dto);

    var opt = tradeRepository.findByTradeId("P-100");
    assert (opt.isPresent());
    var entity = opt.get();
    assert (entity.getTradeId().equals("P-100"));
    assert (entity.getVersion() == 1);

    List<TradeHistory> histories = tradeHistoryRepository.findByTradeIdOrderByVersionDesc("P-100");
    // there should be at least one history document for the create
    assert (histories != null && histories.size() >= 1);
  }
}
