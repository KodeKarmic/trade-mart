package com.trademart.tradestore.testconfig;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Integration test configuration that provides no-op expiry-related beans so the trade-store
 * integration tests don't require the full trade-expiry module wiring.
 */
@TestConfiguration
public class TestExpiryConfig {

  @Bean
  public com.trademart.tradeexpiry.service.TradeMaturityValidator tradeMaturityValidator() {
    return maturityDate -> {
      // no-op for integration tests
    };
  }

  @Bean
  public com.trademart.tradeexpiry.repository.TradeRepository tradeExpiryRepository() {
    return Mockito.mock(com.trademart.tradeexpiry.repository.TradeRepository.class);
  }
}
