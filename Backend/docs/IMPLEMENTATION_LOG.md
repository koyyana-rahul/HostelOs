# HostelOS Implementation Log

> Repository correction (2026-10-28): earlier entries in this log incorrectly
> reported that no Git repository existed. A repository exists at the HostelOS
> root, currently on branch `feature/0.1.3-identity-authorization` with commits
> `577a2b2` and `8f7b72b`. The Module 0.1.3A changes are uncommitted. Historical
> module evidence is preserved below, with the specific Git statements corrected.

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
- No authentication or tenancy existed at the time of this module; Module 0.1.3A
  later added backend authentication.
- `/api/v1/system/info` was intentionally the only application API endpoint in
  this module.
- The original governance statement was incorrect: a Git repository exists. No
  module commit was created during this historical module.

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
- No Git repository was initialized by that stabilization work; the repository
  already existed. Nothing was committed during the stabilization.

## 2026-10-28 — Module 0.1.3A: Identity and Authentication Foundation

Status: Complete on the backend; frontend integration not started.

### Scope

Implemented server-side session authentication and the identity persistence
foundation:

- `GET /api/v1/auth/csrf`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- Flyway `V2` identity schema
- Environment-driven administrator bootstrap
- CSRF protection and bounded login throttling

The existing `GET /api/v1/system/info` contract was preserved.

### Architecture

- Spring Security session cookie `HOSTELOS_SESSION`; no JWT.
- HttpOnly session cookie, `SameSite=Lax`, 30-minute idle timeout.
- Secure session and CSRF cookies under the `prod` profile.
- Session ID rotation on login; logout invalidates the server session.
- CSRF: `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` header.
- CSRF is required for login and authenticated state-changing requests.
- Unauthenticated protected requests still return JSON `401`.
- Deny-by-default authorization and explicit auth routes remain in place.
- BCrypt cost 12; roles `ADMIN` and `STAFF`; `ACTIVE`/`DISABLED` status.
- Normalization uses Unicode NFKC, trim, and lower-case.
- Login limiter: 5 failures per 15 minutes, keyed by normalized login plus
  remote address, bounded in-memory state.
- No default administrator is seeded.
- Administrator bootstrap is disabled by default and requires all four
  `HOSTELOS_BOOTSTRAP_ADMIN_*` values.

The decision is recorded in `docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md`.

### Files added

| File | Purpose |
|---|---|
| `src/main/java/com/rahul/HostelOs/auth/AuthController.java` | Auth endpoints |
| `src/main/java/com/rahul/HostelOs/auth/AuthUserResponse.java` | Safe identity DTO |
| `src/main/java/com/rahul/HostelOs/auth/CsrfResponse.java` | CSRF bootstrap DTO |
| `src/main/java/com/rahul/HostelOs/auth/LoginRequest.java` | Login request DTO |
| `src/main/java/com/rahul/HostelOs/identity/` | Entities, repositories, principal, services, limiter, filter, bootstrap |
| `src/main/resources/db/migration/V2__identity_foundation.sql` | Identity schema and roles |
| `src/test/java/com/rahul/HostelOs/AuthIntegrationTests.java` | Auth/CSRF/session/security integration tests |
| `src/test/java/com/rahul/HostelOs/identity/AdminBootstrapRunnerTests.java` | Bootstrap tests |
| `src/test/java/com/rahul/HostelOs/identity/LoginIdentifierTests.java` | Normalization tests |
| `docs/HOSTELOS_MODULE_0.1.3A_REPORT.md` | Module evidence |
| `docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md` | Architecture decision |

### Files modified

| File | Change |
|---|---|
| `security/SecurityConfig.java` | Session auth, CSRF, BCrypt, active filter, explicit auth routes |
| `config/OpenApiConfig.java` | `sessionAuth` cookie security scheme |
| `common/exception/GlobalExceptionHandler.java` | Explicit JSON `403` mapping |
| `application.properties` | Session, CSRF, bootstrap settings |
| `application-prod.properties` | Secure session and CSRF cookies |
| `.env.example` | Disabled bootstrap placeholders |
| `HostelOsApplicationTests` | Auth/OpenAPI contract coverage |
| `ProductionDocumentationTests` | Production cookie/documentation coverage |
| `docs/API.md` | Auth API and security contract |

No frontend source file was changed.

### Verification

```text
Command: .\mvnw.cmd -B clean verify
Result: BUILD SUCCESS
Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
Test classes: AuthIntegrationTests 13, ErrorHandlingIntegrationTests 2,
HostelOsApplicationTests 12, AdminBootstrapRunnerTests 4,
LoginIdentifierTests 2, ProductionDocumentationTests 7
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar
Jar size: 66,751,865 bytes
JDK: Microsoft OpenJDK 17.0.16
Maven: Wrapper 3.9.9
```

MySQL 8.0.40 runtime verification used a fresh temporary database:

```text
Flyway V1 + V2: PASS
Hibernate validate: PASS
Bootstrap administrator: created once
Roles: ADMIN, STAFF
Schema version: 2
GET /actuator/health: 200
GET /api/v1/system/info: 200
GET /api/v1/auth/csrf: 200
POST /api/v1/auth/login: 200
GET /api/v1/auth/me: 200
POST /api/v1/auth/logout: 204
GET /api/v1/auth/me after logout: 401
Temporary database dropped; existing hostelos database untouched
```

### Database and repository safety

- No existing database was reset, cleaned, or modified.
- Temporary verification schema was dropped.
- `Backend/.env` was not printed or modified.
- Git repository exists; no commit was created for this module.
- Changes are uncommitted for review.
- Automated tests still use H2 MySQL mode; the MySQL check was a one-off local
  runtime verification, not a repeatable integration suite.
- No frontend changes and no new backend dependencies were added.

## 2026-10-28 - Module 0.1.3A-R: Platform Administration Role Reconciliation

Status: Complete

### Scope

Replaced the legacy two-role identity model with three distinct global roles
without adding organization APIs or claiming tenant isolation:

| Role | Meaning |
|---|---|
| `PLATFORM_ADMIN` | Platform-wide identity and privileged configuration |
| `ORGANIZATION_ADMIN` | Reserved for future organization administration; tenant enforcement deferred |
| `STAFF` | Operational staff |

### Existing ADMIN determination

`ADMIN` was the only privileged role, was assigned by the bootstrap, and existed
when no organization or tenant model was present. It therefore meant
platform-wide administration and was mapped to `PLATFORM_ADMIN`.

### Migration

- Added forward-only `V3__reconcile_platform_admin_role.sql`.
- Renamed `ADMIN` to `PLATFORM_ADMIN` in place, preserving role IDs and all
  `user_roles` assignments.
- Inserted `ORGANIZATION_ADMIN`; kept `STAFF` unchanged.
- Replaced the role-name check constraint.
- Deleted no users, role assignments, or role rows.
- Did not edit `V2`.

### Bootstrap and authorization

- Bootstrap remains disabled by default and requires all
  `HOSTELOS_BOOTSTRAP_ADMIN_*` values.
- When enabled, it creates only an active `PLATFORM_ADMIN`.
- Roles remain server-controlled; login payload role fields are ignored.
- No implicit hierarchy exists between platform, organization, and staff roles.
- Session, CSRF, account-status, throttling, and deny-by-default behavior were
  not changed.

### Files changed

| File | Change |
|---|---|
| `src/main/java/com/rahul/HostelOs/identity/RoleName.java` | Three-role enum |
| `src/main/java/com/rahul/HostelOs/identity/UserAccountService.java` | Platform-admin bootstrap assignment |
| `src/main/java/com/rahul/HostelOs/identity/AdminBootstrapRunner.java` | Calls platform-admin bootstrap |
| `src/main/java/com/rahul/HostelOs/auth/AuthUserResponse.java` | Role example updated |
| `src/main/java/com/rahul/HostelOs/auth/LoginRequest.java` | Login example updated; request shape unchanged |
| `src/main/resources/db/migration/V3__reconcile_platform_admin_role.sql` | Forward-only role migration |
| `src/test/java/com/rahul/HostelOs/AuthIntegrationTests.java` | Seeding, role-input, and three-role boundary tests |
| `src/test/java/com/rahul/HostelOs/identity/AdminBootstrapRunnerTests.java` | Updated bootstrap operation |
| `src/test/java/com/rahul/HostelOs/identity/RoleMigrationTests.java` | Fresh and V2-to-V3 upgrade tests |
| `docs/API.md`, `docs/IMPLEMENTATION_LOG.md`, `docs/HOSTELOS_MODULE_0.1.3A_REPORT.md` | Backend documentation |
| `docs/HOSTELOS_MODULE_0.1.3A-R_ROLE_RECONCILIATION_REPORT.md` | Module evidence |
| Shared root docs | ADR, current state, architecture, roadmap, implementation log |

No frontend source was changed.

### Verification

```text
Command: .\mvnw.cmd -B clean verify
Result: BUILD SUCCESS
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
Test classes: AuthIntegrationTests 15, ErrorHandlingIntegrationTests 2,
HostelOsApplicationTests 12, AdminBootstrapRunnerTests 4,
LoginIdentifierTests 2, RoleMigrationTests 2,
ProductionDocumentationTests 7
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar (66,752,614 bytes)
JDK: Microsoft OpenJDK 17.0.16
```

Disposable MySQL 8.0.40 verification:

```text
V1+V2 applied to a disposable schema
Legacy ADMIN user and assignment inserted
V3 applied over the existing data
Roles: ORGANIZATION_ADMIN, PLATFORM_ADMIN, STAFF
Legacy assignment preserved as PLATFORM_ADMIN
Bootstrap account created with PLATFORM_ADMIN only
Flyway V1, V2, V3 all successful
Hibernate validate: PASS
Disposable schema dropped; normal hostelos database untouched
```

### Remaining risks

- Organization membership, tenant boundaries, and organization-scoped
  enforcement are not implemented.
- Existing `ADMIN` accounts become platform administrators and must be reviewed
  before deployment.
- Existing sessions retain old role names until re-login.
- Frontend fixtures still used `ADMIN`/`STAFF` when this backend module
  completed; Module 0.1.3B-R reconciled them separately. This module did not
  edit frontend source.
- MySQL verification remains manual rather than an automated MySQL suite.
- No role-management API or audit trail exists.
