package com.trademart.tradestore.config;

import java.util.Collections;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf().disable();

    // Basic resource-server config: require JWT auth and scope 'trade.ingest' for
    // POST /trades
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/trades")
                .hasAuthority("SCOPE_trade.ingest")
                .anyRequest()
                .permitAll());

    http.oauth2ResourceServer(oauth2 -> oauth2.jwt());

    return http.build();
  }

  // Provide a permissive JwtDecoder in the 'dev' profile only. Integration tests
  // should supply their own `JwtDecoder` (see TestJwtDecoderConfig in the
  // integrationTest source set). Keeping the permissive decoder limited to 'dev'
  // avoids accidental use in production.
  @Bean
  @Profile("dev")
  public JwtDecoder permissiveJwtDecoder() {
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

        // default: valid token with required scope
        Map<String, Object> headers = Map.of("alg", "none");
        Map<String, Object> c = Map.of("scope", "trade.ingest", "sub", "test-user");
        return new Jwt(token, null, null, headers, c);
      }
    };
  }
}
