package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HealthControllerTest {

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Test
  void healthEndpoint() {
    var resp = restTemplate.getForObject("http://localhost:" + port + "/health", String.class);
    assertThat(resp).contains("UP");
  }
}
