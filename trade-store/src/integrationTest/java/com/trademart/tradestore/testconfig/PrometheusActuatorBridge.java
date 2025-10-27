package com.trademart.tradestore.testconfig;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.context.annotation.Bean;

/**
 * Test-only bridge to expose the Prometheus scrape at /actuator/prometheus when
 * the actuator endpoint isn't reachable in the test environment.
 */
@TestConfiguration
public class PrometheusActuatorBridge {

  @Bean
  @ConditionalOnBean(PrometheusMeterRegistry.class)
  public PrometheusController prometheusController(PrometheusMeterRegistry registry) {
    return new PrometheusController(registry);
  }

  @org.springframework.web.bind.annotation.RestController
  static class PrometheusController {
    private final PrometheusMeterRegistry registry;

    PrometheusController(PrometheusMeterRegistry registry) {
      this.registry = registry;
    }

    @GetMapping(value = "/actuator/prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    public String scrape() {
      return registry.scrape();
    }
  }
}
