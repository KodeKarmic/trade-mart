package com.trademart.tradestore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.trademart.tradestore.exception.TradeValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.slf4j.MDC;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionConfig {

  private static final Logger log = LoggerFactory.getLogger(ExceptionConfig.class);

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGeneric(Exception ex) {
    log.error("Unhandled exception caught:", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Internal server error");
  }

  @ExceptionHandler(TradeValidationException.class)
  public ResponseEntity<String> handleValidation(TradeValidationException ex) {
    log.debug("Trade validation failed: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleMethodArgNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    var errors = ex.getBindingResult().getFieldErrors().stream()
        .map(f -> java.util.Map.of("field", f.getField(), "message", f.getDefaultMessage()))
        .collect(Collectors.toList());

    // Build response map allowing nulls for optional fields (traceId)
    Map<String, Object> body = new LinkedHashMap<>();
    // truncate to milliseconds so we always have exactly 3 fractional digits
    String timestamp = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    body.put("timestamp", timestamp);
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("path", request.getRequestURI());
    body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    body.put("message", "Validation failed");
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    body.put("traceId", traceId);
    body.put("errors", errors);

    log.debug("Request body validation failed: {}", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(com.trademart.tradestore.exception.TradeRejectedException.class)
  public ResponseEntity<String> handleRejected(com.trademart.tradestore.exception.TradeRejectedException ex) {
    log.debug("Trade rejected: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }
}
