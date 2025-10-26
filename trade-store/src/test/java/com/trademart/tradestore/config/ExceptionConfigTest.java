package com.trademart.tradestore.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.trademart.tradestore.exception.TradeRejectedException;
import com.trademart.tradestore.exception.TradeValidationException;

public class ExceptionConfigTest {

  @Test
  void rejectedException_mapsVersionToErrorCode() {
    ExceptionConfig cfg = new ExceptionConfig();
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRequestURI("/trades");

    TradeRejectedException ex = new TradeRejectedException("incoming version is lower than existing");
    var resp = cfg.handleRejected(ex, req);

    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    assertTrue(resp.getBody() instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) resp.getBody();

    assertEquals("VERSION_TOO_LOW", body.get("errorCode"));
    assertEquals("incoming version is lower than existing", body.get("message"));
    assertEquals("/trades", body.get("path"));
  }

  @Test
  void rejectedException_mapsMaturityToErrorCode() {
    ExceptionConfig cfg = new ExceptionConfig();
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRequestURI("/trades");

    TradeRejectedException ex = new TradeRejectedException("maturity date is in the past");
    var resp = cfg.handleRejected(ex, req);

    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    assertTrue(resp.getBody() instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) resp.getBody();

    assertEquals("MATURITY_PAST", body.get("errorCode"));
    assertEquals("maturity date is in the past", body.get("message"));
    assertEquals("/trades", body.get("path"));
  }

  @Test
  void whenNoMdcTraceId_thenResponseContainsGeneratedTraceId() {
    // ensure MDC has no pre-existing traceId
    MDC.clear();

    // prepare a simple BindingResult with a field error
    BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "object");
    FieldError fe = new FieldError("object", "field", "must not be null");
    br.addError(fe);

    // mock MethodArgumentNotValidException to return our BindingResult
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(br);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getRequestURI()).thenReturn("/trades");

    ExceptionConfig cfg = new ExceptionConfig();
    var resp = cfg.handleMethodArgNotValid(ex, req);

    assertNotNull(resp);
    assertEquals(400, resp.getStatusCodeValue());

    Object body = resp.getBody();
    assertTrue(body instanceof Map, "response body should be a Map");
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) body;

    assertTrue(map.containsKey("traceId"), "traceId should be present in the error body");
    Object tid = map.get("traceId");
    assertNotNull(tid, "traceId should not be null");
    assertTrue(tid instanceof String, "traceId should be a string");
    String traceId = ((String) tid).trim();
    assertFalse(traceId.isEmpty(), "traceId should not be empty");

    // ensure it's a valid UUID string
    UUID parsed = UUID.fromString(traceId);
    assertNotNull(parsed);
  }

  @Test
  void validationException_mapsToErrorCode() {
    ExceptionConfig cfg = new ExceptionConfig();
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRequestURI("/trades");

    TradeValidationException ex = new TradeValidationException("incoming version is lower than existing");
    var resp = cfg.handleValidation(ex, req);

    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    assertTrue(resp.getBody() instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) resp.getBody();

    assertEquals("VERSION_TOO_LOW", body.get("errorCode"));
    assertEquals("incoming version is lower than existing", body.get("message"));
    assertEquals("/trades", body.get("path"));
  }

  @Test
  void genericException_returnsStructuredInternalError() {
    ExceptionConfig cfg = new ExceptionConfig();
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRequestURI("/internal");

    RuntimeException ex = new RuntimeException("boom");
    var resp = cfg.handleGeneric(ex, req);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    assertTrue(resp.getBody() instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) resp.getBody();

    assertEquals("INTERNAL_ERROR", body.get("errorCode"));
    assertEquals("Internal server error", body.get("message"));
    assertEquals("/internal", body.get("path"));
  }
}
