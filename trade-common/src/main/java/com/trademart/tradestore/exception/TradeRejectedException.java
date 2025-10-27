package com.trademart.tradestore.exception;

/** Used when an incoming trade is explicitly rejected for business reasons. */
public class TradeRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TradeRejectedException(String message) {
    super(message);
  }

  public TradeRejectedException(String message, Throwable cause) {
    super(message, cause);
  }
}
