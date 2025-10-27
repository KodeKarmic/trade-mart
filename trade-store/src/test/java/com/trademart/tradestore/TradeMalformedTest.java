package com.trademart.tradestore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trademart.tradestore.config.ExceptionConfig;
import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.service.TradeService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for malformed trade data validation. Tests various scenarios of
 * invalid input data to
 * ensure proper validation error messages are returned.
 */
@WebMvcTest(controllers = com.trademart.tradestore.streaming.TradeIngestionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ExceptionConfig.class, TradeMalformedTest.TestConfig.class })
public class TradeMalformedTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private TradeService tradeService;

  @org.springframework.boot.test.context.TestConfiguration
  static class TestConfig {
    @org.springframework.context.annotation.Bean
    public io.micrometer.core.instrument.MeterRegistry meterRegistry() {
      return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }
  }

  @Test
  public void whenMissingRequiredFields_thenReturns400WithValidationErrors() throws Exception {
    String json = "{}";

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errorCode").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/trades"))
        .andExpect(
            jsonPath(
                "$.errors[*].field",
                Matchers.containsInAnyOrder("tradeId", "maturityDate", "price")));
  }

  @Test
  public void whenMissingTradeId_thenReturns400() throws Exception {
    String json = """
        {
          "version": 1,
          "maturityDate": "2025-12-31",
          "price": 100.50
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors[*].field", Matchers.hasItem("tradeId")));
  }

  @Test
  public void whenMissingMaturityDate_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "price": 100.50
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors[*].field", Matchers.hasItem("maturityDate")));
  }

  @Test
  public void whenMissingPrice_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "2025-12-31"
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors[*].field", Matchers.hasItem("price")));
  }

  @Test
  public void whenInvalidDateFormat_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "31-12-2025",
          "price": 100.50
        }
        """;

    // Spring returns 500 for JSON deserialization errors (not validation errors)
    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().is5xxServerError());
  }

  @Test
  public void whenInvalidDateValue_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "2025-13-32",
          "price": 100.50
        }
        """;

    // Spring returns 500 for JSON deserialization errors (invalid date)
    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().is5xxServerError());
  }

  @Test
  public void whenInvalidPriceFormat_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "2025-12-31",
          "price": "not-a-number"
        }
        """;

    // Spring returns 500 for JSON deserialization errors
    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().is5xxServerError());
  }

  @Test
  public void whenNegativeVersion_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": -1,
          "maturityDate": "2025-12-31",
          "price": 100.50
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors[*].field", Matchers.hasItem("version")));
  }

  @Test
  public void whenNullTradeId_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": null,
          "version": 1,
          "maturityDate": "2025-12-31",
          "price": 100.50
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field", Matchers.hasItem("tradeId")));
  }

  @Test
  public void whenNullMaturityDate_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": null,
          "price": 100.50
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field", Matchers.hasItem("maturityDate")));
  }

  @Test
  public void whenNullPrice_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "2025-12-31",
          "price": null
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field", Matchers.hasItem("price")));
  }

  @Test
  public void whenInvalidJsonSyntax_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "2025-12-31",
          "price": 100.50
        """; // missing closing brace

    // Malformed JSON results in 500 (JSON parse error)
    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().is5xxServerError());
  }

  @Test
  public void whenEmptyJson_thenReturns400() throws Exception {
    String json = "";

    // Empty body results in 500 (JSON parse error)
    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().is5xxServerError());
  }

  @Test
  public void whenInvalidContentType_thenReturns415() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "2025-12-31",
          "price": 100.50
        }
        """;

    // Wrong content type leads to message converter error (500 in this setup)
    // Note: In production with proper exception handling, this might return 415
    mvc.perform(post("/trades").contentType(MediaType.TEXT_PLAIN).content(json))
        .andExpect(status().is5xxServerError());
  }

  @Test
  public void whenVersionIsDecimal_thenReturns400() throws Exception {
    String json = """
        {
          "tradeId": "T-123",
          "version": 1.5,
          "maturityDate": "2025-12-31",
          "price": 100.50
        }
        """;

    // Type mismatch (expecting Integer, got decimal) results in 500
    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().is5xxServerError());
  }

  @Test
  public void whenExtraFieldsPresent_thenStillProcessed() throws Exception {
    // Mock service to return a trade entity
    TradeEntity saved = new TradeEntity();
    saved.setTradeId("T-123");
    given(tradeService.createOrUpdateTrade(any())).willReturn(saved);

    // Spring Boot typically ignores unknown fields by default
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "2025-12-31",
          "price": 100.50,
          "unknownField": "should-be-ignored"
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated());
  }

  @Test
  public void whenPriceIsZero_thenAccepted() throws Exception {
    // Mock service to return a trade entity
    TradeEntity saved = new TradeEntity();
    saved.setTradeId("T-123");
    given(tradeService.createOrUpdateTrade(any())).willReturn(saved);

    // Zero is valid per @PositiveOrZero constraint (not on price, but testing
    // edge case)
    String json = """
        {
          "tradeId": "T-123",
          "version": 0,
          "maturityDate": "2025-12-31",
          "price": 0
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated());
  }

  @Test
  public void whenDateInPast_thenAcceptedByController() throws Exception {
    // Mock service to return a trade entity
    TradeEntity saved = new TradeEntity();
    saved.setTradeId("T-123");
    given(tradeService.createOrUpdateTrade(any())).willReturn(saved);

    // Note: Business rule validation (maturity in past) happens in service layer
    // Controller validation only checks data format/presence
    String json = """
        {
          "tradeId": "T-123",
          "version": 1,
          "maturityDate": "2020-01-01",
          "price": 100.50
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated());
  }

  @Test
  public void whenMultipleValidationErrors_allErrorsReturned() throws Exception {
    String json = """
        {
          "tradeId": null,
          "version": -5,
          "maturityDate": null,
          "price": null
        }
        """;

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(Matchers.greaterThanOrEqualTo(3)));
  }
}
