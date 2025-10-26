```markdown
# Tasks: Trade Store Validation

**Input**: Design documents from `/specs/002-trade-store-validation/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Create project structure per implementation plan
 - [x] T002 Initialize Java project with Maven or Gradle (target Java 17). Add dependencies: Spring Boot (web, kafka), Spring Data JPA, PostgreSQL driver, MongoDB driver, JUnit 5
- [ ] T003 [P] Configure linting and formatting tools (Checkstyle, Spotless) and add pre-commit hooks for formatting and tests
- [ ] T004 [P] Set up logging framework in src/main/java/com/trademart/app/config/LoggingConfig.java

---

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T005 Set up database schema and migrations (Flyway or Liquibase) and repository modules in src/main/java/com/trademart/app/repository/
- [ ] T006 [P] Implement exception handling infrastructure in src/main/java/com/trademart/app/config/ExceptionConfig.java
- [ ] T007 [P] Configure system clock validation in src/main/java/com/trademart/app/service/ClockService.java

- [ ] T008 [P] Create performance testing harness (Gatling or JMH) in performance/

---

## Phase 3: User Story 1 - Trade Ingestion and Sequencing (Priority: P1)

**Goal**: Trades are received and stored in a specific sequence
**Independent Test**: Send a batch of trades and verify correct sequence

### Tests for User Story 1

 - [x] T009 [P] [US1] Unit test for trade ingestion in src/test/java/com/trademart/app/unit/TradeIngestionTest.java
- [ ] T010 [P] [US1] Integration test for sequencing logic in src/test/java/com/trademart/app/integration/TradeSequencingTest.java

### Implementation for User Story 1

 - [x] T011 [P] [US1] Create Trade model in src/main/java/com/trademart/app/model/Trade.java
- [ ] T012 [US1] Implement sequencing logic in src/main/java/com/trademart/app/service/TradeSequencer.java
 - [x] T013 [US1] Implement ingestion controller in src/main/java/com/trademart/app/streaming/TradeIngestionController.java

---

## Phase 4: User Story 2 - Trade Version Validation (Priority: P2)

**Goal**: Validate trade version and handle replacements/rejections
**Independent Test**: Submit trades with varying versions and verify correct handling


### Tests for User Story 2

- [ ] T014 [P] [US2] Unit test for version validation in src/test/java/com/trademart/app/unit/TradeVersionTest.java
- [ ] T015 [P] [US2] Integration test for replacement/rejection logic in src/test/java/com/trademart/app/integration/TradeVersionIntegrationTest.java

### Implementation for User Story 2

- [ ] T016 [P] [US2] Implement version validation logic in src/main/java/com/trademart/app/service/TradeVersionValidator.java
- [ ] T017 [US2] Implement replacement logic in src/main/java/com/trademart/app/service/TradeStore.java
- [ ] T018 [US2] Implement exception generation for rejected trades in src/main/java/com/trademart/app/service/TradeExceptionService.java

---

## Phase 5: User Story 3 - Maturity Date Validation and Expiry (Priority: P3)

**Goal**: Validate maturity date and mark trades as expired
**Independent Test**: Submit trades with various maturity dates and verify correct handling


### Tests for User Story 3

- [ ] T019 [P] [US3] Unit test for maturity date validation in src/test/java/com/trademart/app/unit/TradeMaturityTest.java
- [ ] T020 [P] [US3] Integration test for expiry marking in src/test/java/com/trademart/app/integration/TradeExpiryIntegrationTest.java

### Implementation for User Story 3

- [ ] T021 [P] [US3] Implement maturity date validation logic in src/main/java/com/trademart/app/service/TradeMaturityValidator.java
- [ ] T022 [US3] Implement expiry marking logic in src/main/java/com/trademart/app/service/TradeExpiryService.java

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T023 [P] Documentation updates in docs/
- [ ] T024 Code cleanup and refactoring
- [ ] T025 Performance optimization in src/main/java/com/trademart/app/service/
- [ ] T026 [P] Additional unit tests in src/test/java/com/trademart/app/unit/
- [ ] T027 Security hardening in src/main/java/com/trademart/app/config/SecurityConfig.java
- [ ] T028 Run quickstart.md validation

## Phase 6.1: Quality & Security (added tasks)

- [ ] T029 Add performance test case in performance/ (Gatling or JMH) and integrate into CI
- [ ] T030 Add vulnerability scan job in .github/workflows/scan.yml using OWASP Dependency-Check or Snyk; fail build on critical/blocker
- [ ] T031 Add CODE_REVIEW requirement: enforce one approving review and checklist validation before merge (document in CONTRIBUTING.md)
- [ ] T032 Add malformed-data test: src/test/java/com/trademart/app/unit/TradeMalformedTest.java and handler in src/main/java/com/trademart/app/service/ValidationService.java
- [ ] T033 Add error-message validation tests in src/test/java/com/trademart/app/integration/ErrorMessageIntegrationTest.java
---

## Dependencies & Execution Order

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

```
# Tasks: Trade Store Validation

**Input**: Design documents from `/specs/002-trade-store-validation/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize Python project with Poetry (create pyproject.toml) and add dependencies: FastAPI, aiokafka, SQLAlchemy, asyncpg, pymongo, pydantic, pytest
- [ ] T003 [P] Configure linting and formatting tools (ruff, black) and pre-commit in pyproject.toml
- [ ] T004 [P] Set up logging framework in src/trademart/config/logging.py

---

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T005 Set up database schema and migrations (alembic) and repository modules in src/trademart/repository/
- [ ] T006 [P] Implement exception handling infrastructure in src/trademart/config/exception_handlers.py
- [ ] T007 [P] Configure system clock validation in src/trademart/services/clock_service.py

- [ ] T008 [P] Create performance testing harness (locustfile or pytest-benchmark) in tests/performance/

---

## Phase 3: User Story 1 - Trade Ingestion and Sequencing (Priority: P1)

**Goal**: Trades are received and stored in a specific sequence
**Independent Test**: Send a batch of trades and verify correct sequence

### Tests for User Story 1

- [ ] T009 [P] [US1] Unit test for trade ingestion in tests/unit/test_trade_ingestion.py
- [ ] T010 [P] [US1] Integration test for sequencing logic in tests/integration/test_trade_sequencing.py

### Implementation for User Story 1

- [ ] T011 [P] [US1] Create Trade Pydantic model in src/trademart/model/trade.py
- [ ] T012 [US1] Implement sequencing logic in src/trademart/services/trade_sequencer.py
- [ ] T013 [US1] Implement ingestion endpoint (FastAPI) in src/trademart/api/ingest.py

---

## Phase 4: User Story 2 - Trade Version Validation (Priority: P2)

**Goal**: Validate trade version and handle replacements/rejections
**Independent Test**: Submit trades with varying versions and verify correct handling


### Tests for User Story 2

- [ ] T014 [P] [US2] Unit test for version validation in tests/unit/test_trade_version.py
- [ ] T015 [P] [US2] Integration test for replacement/rejection logic in tests/integration/test_trade_version_integration.py

### Implementation for User Story 2

- [ ] T016 [P] [US2] Implement version validation logic in src/trademart/services/trade_version_validator.py
- [ ] T017 [US2] Implement replacement logic in src/trademart/services/trade_store.py
- [ ] T018 [US2] Implement structured exceptions for rejected trades in src/trademart/services/exceptions.py

---

## Phase 5: User Story 3 - Maturity Date Validation and Expiry (Priority: P3)

**Goal**: Validate maturity date and mark trades as expired
**Independent Test**: Submit trades with various maturity dates and verify correct handling


### Tests for User Story 3

- [ ] T019 [P] [US3] Unit test for maturity date validation in tests/unit/test_trade_maturity.py
- [ ] T020 [P] [US3] Integration test for expiry marking in tests/integration/test_trade_expiry_integration.py

### Implementation for User Story 3

- [ ] T021 [P] [US3] Implement maturity date validation logic in src/trademart/services/trade_maturity_validator.py
- [ ] T022 [US3] Implement expiry marking logic (scheduler/job) in src/trademart/services/trade_expiry_service.py

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T023 [P] Documentation updates in docs/
- [ ] T024 Code cleanup and refactoring
- [ ] T025 Performance optimization in src/trademart/services/
- [ ] T026 [P] Additional unit tests in tests/unit/
- [ ] T027 Security hardening in src/trademart/config/security.py
- [ ] T028 Run quickstart.md validation

## Phase 6.1: Quality & Security (added tasks)

- [ ] T029 Add performance test case in tests/performance/test_performance.py and integrate into CI
- [ ] T030 Add vulnerability scan job in .github/workflows/scan.yml using OWASP Dependency-Check; fail build on critical/blocker
- [ ] T031 Add CODE_REVIEW requirement: enforce one approving review and checklist validation before merge (document in CONTRIBUTING.md)
- [ ] T032 Add malformed-data test: tests/unit/test_trade_malformed.py and handler in src/trademart/services/validation.py
- [ ] T033 Add error-message validation tests in tests/integration/test_error_messages.py
---

## Dependencies & Execution Order

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
