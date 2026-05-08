# Codebase Concerns

**Analysis Date:** 2026-05-07

## Tech Debt

### 1. Configuration with Hardcoded Secrets (HIGH)

**Issue:** Both `bootstrap.yml` and `bootstrap-template.yml` contain hardcoded production-like credentials, including WeChat enterprise secrets, database passwords, Redis passwords, Tencent Cloud COS secret keys, RabbitMQ credentials, and XXL-Job admin credentials.

**Files:**
- `scrm-server/server/src/main/resources/bootstrap.yml`
- `scrm-server/server/src/main/resources/bootstrap-template.yml`

**Impact:** Any developer with repository access has access to all WeChat enterprise API secrets, database credentials, and cloud storage keys. The `bootstrap-template.yml` is git-tracked with all secrets exposed.

**Fix approach:**
- Remove all secrets from `bootstrap-template.yml`, replace with `${PLACEHOLDER}` values
- Move secrets to environment variables or external secret store
- Rotate all exposed credentials

### 2. Zero Automated Tests (HIGH)

**Issue:** Zero test files exist. No `@Test` annotations, no `*.test.*` files. Maven Surefire configured with `<testFailureIgnore>true</testFailureIgnore>`.

**Files:** Entire codebase -- 906 Java files + 440 frontend source files

**Impact:** No regression safety. Refactoring is high-risk.

**Fix approach:** Add integration tests for WeChat API paths, unit tests for service layer, remove `testFailureIgnore`.

### 3. Massive Service Classes / God Objects (HIGH)

**Issue:** Multiple services exceed recommended size, violating SRP.

**Files:**
- `WxCustomerServiceImpl.java` -- 1818 lines
- `WxMsgTemplateServiceImpl.java` -- 1020 lines
- `StaffServiceImpl.java` -- 1019 lines
- `WxMsgGroupTemplateServiceImpl.java` -- 886 lines
- `WxGroupChatServiceImpl.java` -- 737 lines
- 4 more files over 500 lines

**Fix approach:** Decompose into domain-specific services, extract utility logic, target 300 lines max.

### 4. DTO Bloat in API Module (MEDIUM)

**Files:** `scrm-server/api/src/main/java/com/scrm/api/wx/cp/dto/*.java` (132 files with SaveDTO/UpdateDTO/PageDTO/QueryDTO per entity)

**Fix approach:** Consolidate using partial-update patterns, consider MapStruct.

### 5. Huge Error Code Enum (MEDIUM)

**Files:** `ErrorMsgEnum.java` -- 703 lines with hundreds of WeChat error codes

**Fix approach:** Keep only referenced codes, load from properties file.

### 6. Large Frontend Files (MEDIUM)

19 files over 300 lines, including `GroupDetail/index.js` (660), `DetailPage/index.js` (634).

## Security Considerations

### 1. Credentials in Source Control (HIGH)

WeChat secrets (`extCorpID`, `contactSecret`, `customerSecret`, `mainAgentSecret`), DB password (`YDdev@#215!@#`), Redis (`rd4d0bgO`), COS keys, XXL-Job admin (`password: 123456`) all in tracked YAML files.

### 2. No Spring Security (MEDIUM)

Custom token in `localStorage`, no `@PreAuthorize`, no CSRF, no `@CrossOrigin`. XSS filter only covers limited paths.

### 3. XSS via innerHTML (MEDIUM)

Three React components use `innerHTML` directly with no sanitization.

## Performance Bottlenecks

### 1. No Caching for WeChat API Calls (MEDIUM)

No Redis caching for department/user/tag lists. WeChat rate limits are strict.

### 2. Inefficient Queries (MEDIUM)

1818-line god service with MyBatis Plus queries, potential N+1 patterns.

## Fragile Areas

### 1. WeChat Callback Handler (HIGH)

`WxChangeContactEventHandler.java` and `MsgHandler.java` process real-time WeChat callbacks with TODO comments indicating incomplete implementations. No tests.

### 2. COS File Upload (MEDIUM)

`COSUtils.java` (497 lines) has TODO comments about HTTPS issues and bucket checks.

### 3. Four Frontend Projects (MEDIUM)

React SPA + Vue 3 admin (`mars-admin-ui`) + UniApp mobile + static HTML (`mars-admin-web`). The `mars-admin` directory has its own `.git` repo (not a proper submodule).

## Dependencies at Risk

### 1. Java 8 / Spring Boot 2.3.x (HIGH)

All EOL since Oct 2021. No security patches.

### 2. Fastjson 1.2.83 (HIGH)

Known RCE CVEs. Replace with Jackson or Gson (both already on classpath).

### 3. Webpack 4 / CRA (HIGH)

Webpack 4.44.2 EOL. Requires `NODE_OPTIONS=--openssl-legacy-provider` hack for Node.js 17+.

### 4. MobX 4.x (MEDIUM)

EOL since MobX 6. Legacy decorator syntax.

### 5. Pinyin4j (MEDIUM)

Last updated 2014.

### 6. Braft Editor (MEDIUM)

Unmaintained. Depends on deprecated draft-js.

## Missing Critical Features

### 1. No Observability (HIGH)

No Actuator, no Micrometer, no health checks, no metrics, no log aggregation.

### 2. No CI/CD (HIGH)

No `.github/`, `.gitlab-ci.yml`, or `Jenkinsfile`.

### 3. No API Versioning (MEDIUM)

All APIs under `/api/` without version prefix.

### 4. No Rate Limiting (MEDIUM)

No safeguards against exceeding WeChat API rate limits.

## Test Coverage Gaps

### 1. Complete Absence of Tests (HIGH)

906 Java files + 440 frontend files, zero tests. Any refactoring is extremely high-risk.

## Configuration Management Issues

### 1. Broken Jest Runner Path (MEDIUM)

Hardcoded `"testRunner": "D:\\max-project\\web-template\\..."` -- references a different machine's directory.

### 2. NODE_OPTIONS Legacy Provider (MEDIUM)

All scripts require `--openssl-legacy-provider` -- incompatible with modern Node.js.

### 3. Placeholder CORP_ID (LOW)

`scrm-web/src/config.js` has `CORP_ID = "xxxxxxxxxxxx"`.

### 4. Developer-Specific Paths (LOW)

`linuxFilePath: /Users/lyon/Desktop/` and `windowsFilePath: D:\scrm\` in bootstrap.yml.

### 5. Dual RabbitMQ Port (LOW)

`bootstrap.yml` uses 10808, template shows 5672.

## Code Quality Issues

### 1. Cleanup-Only Git History (MEDIUM)

Last 8 commits are all removals (sidebar, doc, file-h5, product-h5). Suggests feature churn.

### 2. Nested Git Repository (MEDIUM)

`scrm-web/mars-admin/` contains its own `.git` -- not a proper submodule. Causes version tracking confusion.
