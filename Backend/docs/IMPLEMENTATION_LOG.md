# HostelOS Implementation Log

## 2026-10-28 — Module 0.1.2A: System Information API

Status: Complete

### Scope

Implemented one public, versioned, non-sensitive endpoint:

```text
GET /api/v1/system/info
```

The endpoint returns application metadata only and does not query the database.

### Files created

| File | Purpose |
|---|---|
| `src/main/java/com/rahul/HostelOs/system/SystemInfoController.java` | Versioned system-information endpoint |
| `src/main/java/com/rahul/HostelOs/system/SystemInfoResponse.java` | Typed response DTO |
| `docs/API.md` | API contract and CORS/security documentation |
| `docs/IMPLEMENTATION_LOG.md` | This implementation log |

### Files modified

| File | Change |
|---|---|
| `src/main/java/com/rahul/HostelOs/security/SecurityConfig.java` | Permit only `GET /api/v1/system/info` |
| `src/test/java/com/rahul/HostelOs/HostelOsApplicationTests.java` | Added contract and method-restriction tests |

### Contract

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

- HTTP status: `200 OK`
- Content type: `application/json`
- `status` means this system-information endpoint is available.
- `status` is not a claim that every dependency is healthy.
- No environment variables, credentials, database URLs, file paths, or stack traces are returned.

### Security

- The existing deny-by-default chain remains in place.
- Among application routes, only `GET /api/v1/system/info` is permitted.
- Existing infrastructure endpoints such as health, OpenAPI, Swagger UI, and `/error` remain public.
- Other methods on the same path return `401`.
- Unlisted application routes remain denied.
- No `/api/**` wildcard permit was added.
- No authentication provider, fake user, or production authentication behavior was introduced.

### CORS decision

- Development integration uses the existing Vite `/api` proxy.
- No CORS configuration, wildcard origin, or `@CrossOrigin` was added.
- The Vite proxy request is same-origin to the browser.
- A separately hosted frontend and backend will require an explicit production-origin decision and strict origin allowlisting.
- Runtime verification confirmed the proxied response has no `Access-Control-Allow-Origin` header.

### Tests

New tests:

- `systemInfoEndpointIsPublicAndReturnsStableContract`
  - Verifies HTTP 200.
  - Verifies JSON content type.
  - Verifies the exact response body and field values.
- `systemInfoEndpointOnlyPermitsGet`
  - Verifies `POST /api/v1/system/info` returns `401`.

Existing foundation tests retained without replacement:

- Context startup
- Health endpoint behavior
- OpenAPI endpoint
- Swagger UI
- Unlisted endpoint denial
- Validation error contract
- Unexpected-error sanitization

Actual test result:

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

The stack trace logged during the test run is intentional: it is produced by the existing test that verifies internal exception details are not leaked to clients.

### Build verification

Command:

```powershell
$env:JAVA_HOME = 'C:\Users\RAHUL\.jdks\ms-17.0.16'
$env:Path = "C:\Users\RAHUL\.jdks\ms-17.0.16\bin;$env:Path"
.\mvnw.cmd -B clean verify
```

Actual result:

```text
BUILD SUCCESS
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
Total time: 20.164 s
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar
Jar size: 66,717,270 bytes
```

Active toolchain confirmed:

```text
Apache Maven 3.9.9
Java 17.0.16 (Microsoft OpenJDK)
```

### Runtime verification

The packaged application was started on an isolated port with JDK 17.

| Check | Result |
|---|---|
| Backend started | Yes |
| `GET /actuator/health` | `200` |
| `GET /api/v1/system/info` | `200` |
| Response content type | `application/json` |
| Response body | `{"applicationName":"HostelOS","apiVersion":"v1","status":"AVAILABLE"}` |
| `POST /api/v1/system/info` | `401` |
| `GET /api/v1/system/other` | `401` |
| OpenAPI path present | Yes |
| OpenAPI summary | `Get system information` |
| OpenAPI schema present | Yes |
| Vite proxy request | `200` with identical body |
| CORS header through proxy | Absent |

The verification backend and Vite processes were stopped afterward.

### Database safety

- No Flyway migration was added.
- No schema change was made.
- No database reset, repair, clean, or baseline was performed.
- No MySQL user or permission was changed.
- No `.env` credentials were modified.
- Runtime Flyway validation reported the existing schema as up to date at version 1.
- No secret values were printed.

### Known limitations

- The local `.env` still uses the local MySQL `root` account; a least-privilege application user remains a follow-up task.
- IntelliJ may still run with Java 22; the module was built and runtime-verified with Java 17.
- No authentication or tenancy exists yet.
- `/api/v1/system/info` is intentionally the only application API endpoint in this module.
- No Git repository exists; nothing was committed.

### Shared-file changes

- `docs/API.md` was added.
- `docs/IMPLEMENTATION_LOG.md` was added.
- No frontend code was changed.
- No existing foundation code was overwritten or duplicated.

## 2026-10-28 — Module 0.1.2A: Security Test Stabilization

Status: Complete

### Symptom

The first Module 0.1.2 clean verification reported 2 failures out of 9 tests:

```text
POST /api/v1/system/info  expected 401, received 405
GET /api/not-implemented expected 401, received 404
```

Subsequent runs passed without a source change. The first failing Surefire log was not retained, so the original trigger cannot be reconstructed directly.

### Root cause

The failure is a test-context isolation problem, not a production authorization defect.

A controlled reproduction removed the production `securityFilterChain` bean while retaining the test-only `/probe/**` chain. It produced exactly the observed 405 and 404 responses:

- Spring Boot backs off its default security chain when any `SecurityFilterChain` bean exists.
- The probe chain matched only `/probe/**`.
- `/api/**` therefore reached MVC without production authorization; MVC returned 405 for the existing path and 404 for the unlisted path.

Spring Security 7 rejects multiple catch-all chains during context startup, so the original failure was not caused by adding an unscoped probe chain alongside the production chain. The reproduced failure required `/api/**` to bypass the production chain; in the controlled case the production chain bean was absent. The exact reason the production chain was not applied in the first QA run is unknown.

### Fix

No production code or security policy was changed.

- `HostelOsApplicationTests` no longer imports the test probe configuration.
- The test class asserts that exactly one `SecurityFilterChain` exists and that it is the production `SecurityConfig` chain.
- Production security coverage now includes GET, POST, HEAD, PUT, DELETE, PATCH, and an unlisted route.
- Validation and unexpected-error probe tests moved to `ErrorHandlingIntegrationTests`, whose test-only chain is scoped to `/probe/**`.
- `ProductionDocumentationTests` verifies the `prod` profile disables API documentation and Swagger UI while preserving the public system-information and health endpoints.

### Verification

Before the refactor:

```text
Tests: 9
Controlled reproduction: 2 failures with the exact 405/404 symptoms
Pre-fix local full-class runs: 5 passed
Pre-fix random-order runs: 10 passed
```

After the refactor:

```text
Clean verify runs: 4
Tests per clean verify: 19
Result: 19 passed, 0 failed, 0 errors, 0 skipped in every run
Times: 18.338 s, 18.335 s, 18.103 s, 19.270 s
Security-test repeated runs: 20
Tests per repeated run: 17
Result: 17 passed, 0 failed, 0 errors, 0 skipped in every run
Isolated failing-path runs: 5
Tests per isolated run: 2
Result: 2 passed, 0 failed, 0 errors, 0 skipped in every run
```

Final build:

```text
BUILD SUCCESS
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar
Jar size: 66,717,270 bytes
Apache Maven 3.9.9
Java 17.0.16 (Microsoft OpenJDK)
```

The intentional 500-path stack trace is logged by the negative test that verifies internal details are sanitized.

### Release-gate assessment

The fix is sufficient for the Module 0.1.2 gate:

- Production authorization behavior remains exact and deny-by-default.
- The test suite now detects an absent production chain instead of silently exercising a probe-only chain.
- Four clean builds and 20 repeated security runs passed.

The remaining risk is the unproven historical trigger for the first QA failure. CI should retain failing Surefire logs if it recurs.

### Database and repository safety

- No migration, schema change, database reset, or credential change was made.
- No `.env` content was printed or modified.
- No frontend code was changed.
- No Git repository was initialized and nothing was committed.
