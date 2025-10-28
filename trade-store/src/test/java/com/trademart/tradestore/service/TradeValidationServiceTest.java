package com.trademart.tradestore.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.trademart.tradestore.exception.TradeValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TradeValidationServiceTest {

  private TradeValidationService validationService;

  @BeforeEach
  void setup() {
    // Fixed clock at 2025-10-27T00:00:00Z for deterministic tests
    Clock fixed = Clock.fixed(Instant.parse("2025-10-27T00:00:00Z"), ZoneOffset.UTC);
    ClockService clockService = new ClockService(fixed);
    validationService = new TradeValidationService(clockService);
  }

  @Test
  void happyPath_withNullMaturity_doesNotThrow() {
    assertDoesNotThrow(() -> validationService.validateForIngest("T1", 0, null));
  }

  @Test
  void boundary_whenVersionZero_doesNotThrow() {
    // version 0 is a valid boundary value
    assertDoesNotThrow(() -> validationService.validateForIngest("T_BOUND", 0, null));
  }

  @Test
  void invalid_whenTradeIdBlank_throws() {
    assertThrows(
        TradeValidationException.class, () -> validationService.validateForIngest("", 1, null));
  }

  @Test
  void happyPath_whenVeryLongTradeId_doesNotThrow() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 2000; i++)
      sb.append('A');
    String longId = sb.toString();
    assertDoesNotThrow(() -> validationService.validateForIngest(longId, 1, null));
  }

  @Test
  void invalid_whenTradeIdNull_throws() {
    assertThrows(
        TradeValidationException.class, () -> validationService.validateForIngest(null, 1, null));
  }

  @Test
  void invalid_whenVersionNegative_throws() {
    assertThrows(
        TradeValidationException.class, () -> validationService.validateForIngest("T1", -1, null));
  }

  @Test
  void invalid_whenMaturityInPast_throws() {
    LocalDate yesterday = LocalDate.parse("2025-10-26");
    assertThrows(
        TradeValidationException.class,
        () -> validationService.validateForIngest("T1", 1, yesterday));
  }

  @ParameterizedTest
  @ValueSource(ints = { -1000, -10, -1 })
  void parameterized_invalidVersions_throw(int v) {
    assertThrows(
        TradeValidationException.class, () -> validationService.validateForIngest("T_PV", v, null));
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 1, 10, 100, Integer.MAX_VALUE })
  void parameterized_validVersions_doNotThrow(int v) {
    assertDoesNotThrow(() -> validationService.validateForIngest("T_PV_OK", v, null));
  }

  @ParameterizedTest
  @MethodSource("pastDates")
  void parameterized_pastMaturities_throw(LocalDate d) {
    assertThrows(
        TradeValidationException.class, () -> validationService.validateForIngest("T_PD", 1, d));
  }

  @ParameterizedTest
  @MethodSource("futureDates")
  void parameterized_futureMaturities_doNotThrow(LocalDate d) {
    assertDoesNotThrow(() -> validationService.validateForIngest("T_FD", 1, d));
  }

  static Stream<LocalDate> pastDates() {
    return Stream.of(
        LocalDate.parse("2025-10-26"),
        LocalDate.parse("2025-10-17"),
        LocalDate.parse("2024-10-27"));
  }

  static Stream<LocalDate> futureDates() {
    return Stream.of(
        LocalDate.parse("2025-10-27"), // today
        LocalDate.parse("2025-10-28"),
        LocalDate.parse("2026-01-01"));
  }

  @org.junit.jupiter.api.Disabled("Requires DB-backed validation which isn't implemented yet")
  @Test
  void dbLinkedValidation_placeholder() {
    // Placeholder for future DB-linked validation: e.g., check highest version in
    // DB
  }

  @Test
  void invalid_whenVersionNull_throws() {
    assertThrows(
        TradeValidationException.class,
        () -> validationService.validateForIngest("T1", null, null));
  }

  @Test
  void happyPath_whenVersionMax_doesNotThrow() {
    assertDoesNotThrow(() -> validationService.validateForIngest("T_MAX", Integer.MAX_VALUE, null));
  }

  @Test
  void happyPath_whenMaturityIsToday_doesNotThrow() {
    LocalDate today = LocalDate.parse("2025-10-27");
    assertDoesNotThrow(() -> validationService.validateForIngest("T_TODAY", 1, today));
  }

  @Test
  void happyPath_whenMaturityInFuture_doesNotThrow() {
    LocalDate future = LocalDate.parse("2025-12-31");
    assertDoesNotThrow(() -> validationService.validateForIngest("T_FUTURE", 1, future));
  }

  @Test
  void timezone_edge_clockWithZone_doesNotAffectUtcComparison() {
    // Use a clock fixed with a non-UTC zone to ensure validation uses UTC
    // internally
    Clock fixedWithZone = Clock.fixed(Instant.parse("2025-10-27T00:30:00Z"), ZoneId.of("Asia/Kolkata"));
    ClockService cs = new ClockService(fixedWithZone);
    TradeValidationService svc = new TradeValidationService(cs);

    LocalDate today = LocalDate.parse("2025-10-27");
    assertDoesNotThrow(() -> svc.validateForIngest("T_ZONE", 1, today));
  }
}
