package com.trademart.tradestore;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.trademart.tradestore.model.TradeDto;
import com.trademart.tradestore.model.TradeEntity;
import com.trademart.tradestore.service.impl.SimpleTradeVersionValidator;
import org.junit.jupiter.api.Test;

public class TradeVersionValidatorTest {

  private final SimpleTradeVersionValidator validator = new SimpleTradeVersionValidator();

  @Test
  void whenIncomingLower_thanExisting_thenReject() {
    TradeEntity existing = new TradeEntity();
    existing.setVersion(5);

    TradeDto dto = new TradeDto();
    dto.setVersion(4);

    assertThatThrownBy(() -> validator.validate(dto, existing))
        .isInstanceOf(com.trademart.tradestore.exception.TradeRejectedException.class);
  }

  @Test
  void whenIncomingNull_thenAccept() {
    TradeEntity existing = new TradeEntity();
    existing.setVersion(3);

    TradeDto dto = new TradeDto();
    dto.setVersion(null);

    assertThatNoException().isThrownBy(() -> validator.validate(dto, existing));
  }

  @Test
  void whenExistingNull_thenAccept() {
    TradeDto dto = new TradeDto();
    dto.setVersion(1);

    assertThatNoException().isThrownBy(() -> validator.validate(dto, null));
  }
}
