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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = com.trademart.tradestore.streaming.TradeIngestionController.class)
public class TradeIngestionControllerTest {

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper mapper;

  @MockBean private TradeService tradeService;

  @Test
  public void whenInvalidPayload_thenReturns400() throws Exception {
    String invalidJson = "{ \"tradeId\": \"T1\" }"; // missing required fields

    mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
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
}
