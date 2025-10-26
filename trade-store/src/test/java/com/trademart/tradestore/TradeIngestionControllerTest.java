package com.trademart.tradestore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.service.TradeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.trademart.tradestore.config.ExceptionConfig;
import com.trademart.tradestore.exception.TradeRejectedException;

@WebMvcTest(controllers = com.trademart.tradestore.streaming.TradeIngestionController.class)
@Import(ExceptionConfig.class)
public class TradeIngestionControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private TradeService tradeService;

  @Test
  public void whenInvalidPayload_thenReturns400() throws Exception {
    String invalidJson = "{ \"tradeId\": \"T1\" }"; // missing required fields

    // set a trace id in MDC so the advice will include it
    MDC.put("traceId", "test-trace-1");

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value(400))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.path").value("/trades"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error").value("Bad Request"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
            .value("Validation failed"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.traceId")
            .value("test-trace-1"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.timestamp").isNotEmpty())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errors[*].field",
            Matchers.containsInAnyOrder("maturityDate", "price")));

    MDC.remove("traceId");
  }

  @Test
  public void whenInvalidPayload_timestampHasMillis() throws Exception {
    String invalidJson = "{ \"tradeId\": \"T1\" }";
    MDC.put("traceId", "ts-test-1");

    var result = mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest())
        .andReturn();

    String content = result.getResponse().getContentAsString();

    var node = mapper.readTree(content);
    String ts = node.get("timestamp").asText();

    // ISO_OFFSET_DATE_TIME with at least milliseconds (3 fractional digits)
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(
        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|[+-]\\d{2}:\\d{2})$");
    assertTrue(p.matcher(ts).matches(), "timestamp must be ISO_OFFSET_DATE_TIME with milliseconds: " + ts);

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

    var result = mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
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
}
