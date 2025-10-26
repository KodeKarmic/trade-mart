package com.trademart.tradestore.service;

public interface TradeSequencer {

  /**
   * Return the next ingest sequence number (monotonic increasing) used to order
   * incoming trade events.
   */
  long nextSequence();
}
