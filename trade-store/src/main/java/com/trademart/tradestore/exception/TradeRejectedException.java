package com.trademart.tradestore.exception;

/** Thrown when a trade must be rejected due to business rules (e.g., version too low). */
public class TradeRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TradeRejectedException(String message) {
    super(message);
  }

  public TradeRejectedException(String message, Throwable cause) {
    super(message, cause);
  }
}
