package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HealthControllerTest {

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate restTemplate;

  @Test
  void healthEndpoint() {
    var resp = restTemplate.getForObject("http://localhost:" + port + "/health", String.class);
    assertThat(resp).contains("UP");
  }

  @TestConfiguration
  static class TestBeans {
    @Bean
    public com.trademart.tradeexpiry.service.TradeMaturityValidator tradeMaturityValidator() {
      return maturityDate -> {
        // no-op for tests that don't exercise expiry validation
      };
    }

    @Bean
    public com.trademart.tradeexpiry.repository.TradeRepository tradeExpiryRepository() {
      return org.mockito.Mockito.mock(com.trademart.tradeexpiry.repository.TradeRepository.class);
    }
  }
}
