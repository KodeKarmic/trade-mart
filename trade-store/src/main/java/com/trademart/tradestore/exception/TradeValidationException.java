package com.trademart.tradestore.exception;

/**
 * Thrown when an incoming trade fails validation rules.
 */
public class TradeValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TradeValidationException(String message) {
    super(message);
  }

  public TradeValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
