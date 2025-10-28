package com.trademart.tradestore.config;

import com.trademart.tradestore.exception.TradeValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionConfig {

  private static final Logger log = LoggerFactory.getLogger(ExceptionConfig.class);

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneric(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception caught:", ex);

    Map<String, Object> body = new LinkedHashMap<>();
    String timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    body.put("timestamp", timestamp);
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("path", request == null ? null : request.getRequestURI());
    body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    // do not expose internal exception messages to clients
    body.put("message", "Internal server error");
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    body.put("traceId", traceId);

    // machine-readable error code for internal errors
    body.put("errorCode", "INTERNAL_ERROR");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  @ExceptionHandler(TradeValidationException.class)
  public ResponseEntity<Object> handleValidation(
      TradeValidationException ex, HttpServletRequest request) {
    log.debug("Trade validation failed: {}", ex.getMessage());

    Map<String, Object> body = new LinkedHashMap<>();
    String timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    body.put("timestamp", timestamp);
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("path", request.getRequestURI());
    body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    body.put("message", ex.getMessage());
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    body.put("traceId", traceId);

    // provide a machine-readable error code for clients
    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
    String errorCode = "TRADE_REJECTED";
    if (msg.contains("version")) {
      errorCode = "VERSION_TOO_LOW";
    } else if (msg.contains("maturity")) {
      errorCode = "MATURITY_PAST";
    }
    body.put("errorCode", errorCode);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleMethodArgNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    var errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(f -> java.util.Map.of("field", f.getField(), "message", f.getDefaultMessage()))
            .collect(Collectors.toList());

    // Build response map allowing nulls for optional fields (traceId)
    Map<String, Object> body = new LinkedHashMap<>();
    // truncate to milliseconds so we always have exactly 3 fractional digits
    String timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
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
    // provide a machine-readable error code for clients
    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
    String errorCode = "TRADE_REJECTED";
    if (msg.contains("version")) {
      errorCode = "VERSION_TOO_LOW";
    } else if (msg.contains("maturity")) {
      errorCode = "MATURITY_PAST";
    }
    body.put("errorCode", errorCode);
    body.put("errors", errors);

    log.debug("Request body validation failed: {}", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    // treat IllegalArgumentException thrown by validators as a bad request with
    // structured body to preserve existing integration test expectations
    log.debug("Illegal argument (validation) failed: {}", ex.getMessage());

    Map<String, Object> body = new LinkedHashMap<>();
    String timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    body.put("timestamp", timestamp);
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("path", request == null ? null : request.getRequestURI());
    body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    body.put("message", ex.getMessage());
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    body.put("traceId", traceId);

    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
    String errorCode = "TRADE_REJECTED";
    if (msg.contains("version")) {
      errorCode = "VERSION_TOO_LOW";
    } else if (msg.contains("maturity")) {
      errorCode = "MATURITY_PAST";
    }
    body.put("errorCode", errorCode);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(com.trademart.tradestore.exception.TradeRejectedException.class)
  public ResponseEntity<Object> handleRejected(
      com.trademart.tradestore.exception.TradeRejectedException ex, HttpServletRequest request) {
    log.debug("Trade rejected: {}", ex.getMessage());

    Map<String, Object> body = new LinkedHashMap<>();
    String timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    body.put("timestamp", timestamp);
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("path", request.getRequestURI());
    body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    body.put("message", ex.getMessage());
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    body.put("traceId", traceId);

    // provide a machine-readable error code for clients (same mapping as validation
    // handler)
    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
    String errorCode = "TRADE_REJECTED";
    if (msg.contains("version")) {
      errorCode = "VERSION_TOO_LOW";
    } else if (msg.contains("maturity")) {
      errorCode = "MATURITY_PAST";
    }
    body.put("errorCode", errorCode);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
}
