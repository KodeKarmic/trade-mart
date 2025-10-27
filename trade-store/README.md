trade-store
===========

Minimal Spring Boot skeleton for the Trade Store validation service.

Run:

```powershell
./gradlew bootRun
```

Run tests:

```powershell
./gradlew test
```

Security (dev & test)
---------------------

The application secures the ingestion endpoint (`POST /trades`) with OAuth2 JWTs when the
resource-server is enabled. For local development and automated tests we provide two
conveniences:


- Dev-only permissive JwtDecoder: when you run the application with the `dev` profile
  (e.g. `-Dspring.profiles.active=dev` or via IDE run configuration) a small permissive
  `JwtDecoder` bean is registered that accepts simple token strings used for local testing
  (for example `valid-token`, `no-scope`, `expired`). This is intended only for
  development convenience and MUST NOT be used in production.


- Test JwtDecoder (integration tests): the integration tests supply a test-only
  `TestJwtDecoderConfig` (in the `src/integrationTest` source set) which registers a
  deterministic `JwtDecoder` mapping token strings to Jwt claims. Integration tests set a
  Bearer token (for example `valid-token`) on HTTP requests so the secured endpoints can
  be exercised without an external JWKS endpoint.

How to run tests that exercise security
---------------------------------------

The integration tests are configured to import the test Jwt decoder and attach a
`Bearer` token for requests. Run the integration test task as usual:

```powershell
./gradlew integrationTest
```

Production
----------

In production, configure a proper JWKS-backed decoder instead of the dev permissive
decoder by setting `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` to your
identity provider's JWKS endpoint (or provide a custom `JwtDecoder` bean). The dev
permissive decoder is bound to the `dev` profile so it won't be active unless explicitly
enabled.
