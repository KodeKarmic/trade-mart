package com.trademart.tradestore.testconfig;

import java.util.Collections;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Test configuration that provides a simple JwtDecoder for unit tests.
 *
 * Behavior:
 * - token value "no-scope" -> Jwt with no scopes
 * - token value "expired" -> throws JwtException
 * - any other token -> Jwt with scope "trade.ingest"
 */
@Configuration
public class TestJwtDecoderConfig {

  @Bean
  public JwtDecoder jwtDecoder() {
    return new JwtDecoder() {
      @Override
      public Jwt decode(String token) throws JwtException {
        if ("expired".equals(token)) {
          throw new JwtException("token expired");
        }

        Map<String, Object> claims = Map.of("sub", "test-user");
        if ("no-scope".equals(token)) {
          return new Jwt(token, null, null, Collections.emptyMap(), claims);
        }

        // include scope as space-separated string (Spring maps SCOPE_.. authorities)
        Map<String, Object> headers = Map.of("alg", "none");
        Map<String, Object> c = Map.of("scope", "trade.ingest", "sub", "test-user");
        return new Jwt(token, null, null, headers, c);
      }
    };
  }
}
