package com.trademart.tradestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademart.tradestore.config.ExceptionConfig;
import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.service.TradeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = com.trademart.tradestore.streaming.TradeIngestionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ExceptionConfig.class, TradeIngestionControllerTest.TestConfig.class})
public class TradeIngestionControllerTest {

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper mapper;

  @MockBean private TradeService tradeService;

  @Autowired private io.micrometer.core.instrument.MeterRegistry meterRegistry;

  @Test
  public void whenInvalidPayload_thenReturns400() throws Exception {
    String invalidJson = "{ \"tradeId\": \"T1\" }"; // missing required fields

    // set a trace id in MDC so the advice will include it
    MDC.put("traceId", "test-trace-1");

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                .value(400))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.path")
                .value("/trades"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error")
                .value("Bad Request"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                .value("Validation failed"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.traceId")
                .value("test-trace-1"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.timestamp")
                .isNotEmpty())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                "$.errors[*].field", Matchers.containsInAnyOrder("maturityDate", "price")));

    MDC.remove("traceId");
  }

  @Test
  public void whenInvalidPayload_timestampHasMillis() throws Exception {
    String invalidJson = "{ \"tradeId\": \"T1\" }";
    MDC.put("traceId", "ts-test-1");

    var result =
        mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
            .andExpect(status().isBadRequest())
            .andReturn();

    String content = result.getResponse().getContentAsString();

    var node = mapper.readTree(content);
    String ts = node.get("timestamp").asText();

    // ISO_OFFSET_DATE_TIME with at least milliseconds (3 fractional digits)
    java.util.regex.Pattern p =
        java.util.regex.Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|[+-]\\d{2}:\\d{2})$");
    assertTrue(
        p.matcher(ts).matches(), "timestamp must be ISO_OFFSET_DATE_TIME with milliseconds: " + ts);

    // also ensure it can be parsed as an OffsetDateTime
    java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(ts);
    assertNotNull(odt, "parsed OffsetDateTime should not be null");

    MDC.remove("traceId");
  }

  @Test
  public void whenValidPayload_thenReturns201() throws Exception {
    TradeDto dto = new TradeDto();
    dto.setTradeId("T-100");
    dto.setPrice(new BigDecimal("10.5"));
    dto.setMaturityDate(LocalDate.of(2026, 12, 31));

    TradeEntity saved = new TradeEntity();
    saved.setTradeId("T-100");

    given(tradeService.createOrUpdateTrade(any(TradeDto.class))).willReturn(saved);

    String json = mapper.writeValueAsString(dto);

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated());
  }

  @Test
  public void whenServiceRejects_thenReturns400() throws Exception {
    TradeDto dto = new TradeDto();
    dto.setTradeId("T-REJ");
    dto.setPrice(new BigDecimal("1.00"));
    dto.setMaturityDate(LocalDate.now().plusDays(1));

    given(tradeService.createOrUpdateTrade(any(TradeDto.class)))
        .willThrow(new TradeRejectedException("incoming version is lower than existing"));

    String json = mapper.writeValueAsString(dto);

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void whenNoMdc_thenTraceIdIsGeneratedInResponse() throws Exception {
    // ensure no traceId in MDC
    MDC.remove("traceId");

    String invalidJson = "{ \"tradeId\": \"T1\" }";

    var result =
        mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
            .andExpect(status().isBadRequest())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    var node = mapper.readTree(content);

    assertTrue(node.has("traceId"));
    String traceId = node.get("traceId").asText();
    assertNotNull(traceId);
    assertFalse(traceId.isBlank());

    // should be a valid UUID string (will throw IllegalArgumentException if
    // invalid)
    UUID.fromString(traceId);
  }

  @Test
  public void whenValidPayload_metricsAreRecorded() throws Exception {
    TradeDto dto = new TradeDto();
    dto.setTradeId("T-MET-1");
    dto.setPrice(new BigDecimal("10.5"));
    dto.setMaturityDate(LocalDate.of(2026, 12, 31));

    TradeEntity saved = new TradeEntity();
    saved.setTradeId("T-MET-1");

    given(tradeService.createOrUpdateTrade(any(TradeDto.class))).willReturn(saved);

    // ensure counters start at 0
    var reqCounter = meterRegistry.counter("trade_ingest_requests_total");
    var errCounter = meterRegistry.counter("trade_ingest_errors_total");
    var timer = meterRegistry.find("trade_ingest_latency_seconds").timer();

    double beforeReq = reqCounter != null ? reqCounter.count() : 0.0;
    double beforeErr = errCounter != null ? errCounter.count() : 0.0;
    long beforeTimerCount = timer != null ? timer.count() : 0L;

    String json = mapper.writeValueAsString(dto);

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated());

    // counters/timer should have incremented
    assertTrue(meterRegistry.counter("trade_ingest_requests_total").count() >= beforeReq + 1.0);
    assertEquals(beforeErr, meterRegistry.counter("trade_ingest_errors_total").count(), 0.0001);
    var afterTimer = meterRegistry.find("trade_ingest_latency_seconds").timer();
    assertNotNull(afterTimer);
    assertTrue(afterTimer.count() >= beforeTimerCount + 1);
  }

  @Test
  public void whenServiceThrows_errorCounterIncrements() throws Exception {
    TradeDto dto = new TradeDto();
    dto.setTradeId("T-MET-ERR");
    dto.setPrice(new BigDecimal("1.00"));
    dto.setMaturityDate(LocalDate.now().plusDays(1));

    given(tradeService.createOrUpdateTrade(any(TradeDto.class)))
        .willThrow(new RuntimeException("boom"));

    var errCounter = meterRegistry.counter("trade_ingest_errors_total");
    double beforeErr = errCounter != null ? errCounter.count() : 0.0;

    String json = mapper.writeValueAsString(dto);

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().is5xxServerError());

    assertTrue(meterRegistry.counter("trade_ingest_errors_total").count() >= beforeErr + 1.0);
  }

  @org.springframework.boot.test.context.TestConfiguration
  static class TestConfig {
    @org.springframework.context.annotation.Bean
    public io.micrometer.core.instrument.MeterRegistry meterRegistry() {
      return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }
  }
}
