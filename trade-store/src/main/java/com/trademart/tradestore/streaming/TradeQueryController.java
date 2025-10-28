package com.trademart.tradestore.streaming;

import com.trademart.tradeexpiry.repository.TradeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
public class TradeQueryController {

  private final TradeRepository tradeRepository;

  public TradeQueryController(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  /**
   * Return the current max version for a given tradeId. Returns 204 No Content when no versions
   * exist yet for that tradeId.
   */
  @GetMapping("/{tradeId}/max-version")
  public ResponseEntity<Integer> maxVersion(@PathVariable String tradeId) {
    Integer max = tradeRepository.findMaxVersionByTradeId(tradeId);
    if (max == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(max);
  }
}
