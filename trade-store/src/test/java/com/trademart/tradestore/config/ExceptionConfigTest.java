package com.trademart.tradestore.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExceptionConfigTest {

  @Test
  void whenNoMdcTraceId_thenResponseContainsGeneratedTraceId() throws NoSuchMethodException {
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
}
