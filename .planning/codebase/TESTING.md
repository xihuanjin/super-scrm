# Testing Practices

**Analysis Date:** 2026/05/07

## Executive Summary

**This project has ZERO automated tests.** No unit tests, no integration tests, no end-to-end tests exist for either the backend (906 Java files) or frontend (440 source files). Test infrastructure is partially configured but unused.

---

## Current State

### Backend (scrm-server)

**Test Files Found:** 0

Search results:
- `scrm-server/server/src/test/` directory: does not exist
- No `*Test*.java` or `*Tests*.java` files found in any module
- No `@Test` annotations anywhere in the project source

**Test Infrastructure Configured:**
- Maven Surefire Plugin 2.x configured in root `pom.xml`
- ⚠️ `testFailureIgnore` is set to `true` — test failures are silently ignored
- `spring-boot-starter-test` is in dependency management (transitive)
- Test dependencies likely include: JUnit 5 (via Spring Boot 2.3.x), Mockito, AssertJ

### Frontend (scrm-web — React)

**Test Files Found:** 0

Search results:
- No `*.test.js`, `*.test.jsx`, `*.spec.js`, `*.spec.jsx` files
- No `__tests__/` directories

**Test Infrastructure Configured (in package.json):**
```json
{
  "jest": {
    "testRunner": "D:\\max-project\\web-template\\node_modules\\jest-runner.js"
  },
  "devDependencies": {
    "jest": "^26.0.0",
    "@testing-library/react": "^11.1.0",
    "@testing-library/jest-dom": "^5.11.4",
    "@testing-library/user-event": "^12.1.10",
    "eslint-plugin-testing-library": "^3.9.1"
  }
}
```

**⚠️ Issues with test config:**
- `testRunner` path references `D:\max-project\web-template\...` — a different machine's directory, won't work
- Jest 26 is configured but no test script is defined in `scripts`
- ESLint testing-library plugin is installed but has no tests to lint

### Frontend (mars-admin-ui — Vue)

**Test Files Found:** 0

No Vitest or Jest configuration found.

---

## Test Coverage

**Coverage:** 0% across all modules

| Module | Source Files | Test Files | Coverage |
|---|---|---|---|
| scrm-server/api (DTO + Feign) | ~150 | 0 | 0% |
| scrm-server/scrm-common | ~50 | 0 | 0% |
| scrm-server/server (Controller + Service + Mapper) | ~400 | 0 | 0% |
| scrm-web (React SPA) | ~300 | 0 | 0% |
| mars-admin-ui (Vue 3) | ~140 | 0 | 0% |
| **Total** | **~1040** | **0** | **0%** |

---

## Risk Assessment

### Critical Risk Areas Without Tests

1. **WeChat API Integration** — No integration tests verify communication with 企业微信 APIs. Breaking changes in the WeChat SDK or API go undetected.

2. **Customer Sync Logic** — `WxCustomerServiceImpl` (1818 lines) syncs customer data from WeChat. Errors silently corrupt the database.

3. **Callback Event Processing** — 22 RabbitMQ queues process WeChat callbacks. No tests verify event parsing, routing, or idempotency.

4. **Staff On-Job Transfer** — `WxStaffOnJobTransferServiceImpl` handles critical customer reassignment. No regression safety.

5. **COS File Upload** — File upload/URL generation has no tests. Security issues (signed URL exposure) would go unnoticed.

6. **Authentication Flow** — Custom token auth with no test coverage. Token expiry/refresh/hijacking scenarios untested.

---

## Recommendations

### Phase 1: Foundation (Immediate)
1. **Fix Maven Surefire:** Set `testFailureIgnore` to `false`
2. **Fix Jest config:** Remove hardcoded `D:\max-project\...` path
3. **Add a smoke test:** One integration test that loads Spring context
4. **Add CI:** GitHub Actions or Jenkins pipeline that runs tests on PR

### Phase 2: Critical Path Coverage
1. Write integration tests for all WeChat API Feign clients (mock WeChat responses)
2. Write unit tests for top 5 god services (>500 lines)
3. Write integration tests for RabbitMQ event handlers (TestContainers + RabbitMQ)
4. Write contract tests for REST API endpoints

### Phase 3: Steady State
1. Require tests for all new features
2. Add E2E tests for critical user journeys (login → customer management)
3. Add performance tests for API endpoints under load
4. Set coverage thresholds (start at 30% and ratchet up)

### Recommended Tools
- **Backend unit:** JUnit 5 + Mockito + AssertJ
- **Backend integration:** Spring Boot Test + TestContainers (MySQL, Redis, RabbitMQ)
- **Frontend unit:** Jest + React Testing Library
- **Frontend integration:** Cypress or Playwright
- **Coverage:** JaCoCo (Java) + Jest coverage (JS)
- **Mutation testing:** PIT (Java) — stretch goal

---

*Testing analysis: 2026/05/07*
