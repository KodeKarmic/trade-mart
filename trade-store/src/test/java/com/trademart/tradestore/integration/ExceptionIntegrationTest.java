package com.trademart.tradestore.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademart.tradestore.service.TradeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ExceptionIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  @TestConfiguration
  static class TestBeans {
    @Bean
    public TradeService tradeService() {
      // validation errors occur before the service is invoked, so a simple mock is
      // sufficient
      return Mockito.mock(TradeService.class);
    }
  }

  @Test
  public void validationErrorResponse_shouldContainGeneratedTraceId_andTimestamp() throws Exception {
    // ensure MDC has no traceId so the exception handler generates one
    MDC.clear();

    String invalidJson = "{ \"tradeId\": \"T-X\" }";

    var result = mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer test-token")
        .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andReturn();

    String content = result.getResponse().getContentAsString();
    var node = mapper.readTree(content);

    assertTrue(node.has("traceId"), "response should include traceId");
    String traceId = node.get("traceId").asText();
    assertNotNull(traceId);
    assertFalse(traceId.isBlank());
    // must be a valid UUID
    UUID.fromString(traceId);

    assertTrue(node.has("timestamp"));
    String ts = node.get("timestamp").asText();
    // ISO_OFFSET_DATE_TIME with exactly 3 fractional digits
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(
        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|[+-]\\d{2}:\\d{2})$");
    assertTrue(p.matcher(ts).matches(),
        "timestamp must be ISO_OFFSET_DATE_TIME with exactly 3 fractional digits: " + ts);

    // parseable as OffsetDateTime
    java.time.OffsetDateTime.parse(ts);
  }
}
