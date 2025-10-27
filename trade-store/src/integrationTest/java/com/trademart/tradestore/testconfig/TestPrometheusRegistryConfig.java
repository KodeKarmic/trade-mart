package com.trademart.tradestore.testconfig;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestPrometheusRegistryConfig {

  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }
}
