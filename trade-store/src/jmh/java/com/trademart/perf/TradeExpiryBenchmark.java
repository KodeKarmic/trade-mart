package com.trademart.perf;

import java.time.LocalDate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class TradeExpiryBenchmark {

  // Simple synthetic workload that simulates checking maturity dates for expiry
  @Benchmark
  public int checkExpiryLoop() {
    int expired = 0;
    LocalDate today = LocalDate.now();
    for (int i = 0; i < 1000; i++) {
      LocalDate maturity = today.minusDays(i % 30);
      if (maturity.isBefore(today)) {
        expired++;
      }
    }
    return expired;
  }
}
