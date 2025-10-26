package com.trademart.tradestore.streaming;

import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
@Validated
public class TradeIngestionController {
  private final TradeService tradeService;

  public TradeIngestionController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @PostMapping
  public ResponseEntity<String> ingest(@Valid @RequestBody TradeDto trade) {
    // persist trade and write history
    var saved = tradeService.createOrUpdateTrade(trade);
    // return 201 Created with location header pointing to resource
    return ResponseEntity.created(
            org.springframework.web.util.UriComponentsBuilder.fromPath("/trades/{id}")
                .buildAndExpand(saved.getTradeId())
                .toUri())
        .body("created");
  }
}
