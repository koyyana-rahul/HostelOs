# HostelOS Module 0.1.3A Report

- Date: 2026-10-28
- Status: Backend identity/authentication foundation complete; frontend
  integration not started
- Module: 0.1.3A - Identity and Authentication Foundation
- Runtime verified with: Java 17.0.16, Maven 3.9.9, MySQL 8.0.40

> Role model superseded: Module 0.1.3A-R replaced `ADMIN` with
> `PLATFORM_ADMIN`, added `ORGANIZATION_ADMIN`, and introduced Flyway `V3`.
> Statements below about `ADMIN` and the V2 seed data describe the original
> 0.1.3A state. Current behavior is documented in
> `HOSTELOS_MODULE_0.1.3A-R_ROLE_RECONCILIATION_REPORT.md`.

## Outcome

Implemented a backend session-authentication foundation with persisted users,
server-side roles, account status enforcement, CSRF protection, a bounded login
throttle, and an environment-driven administrator bootstrap.

The public Module 0.1.2 endpoint remains unchanged:

```text
GET /api/v1/system/info
```

New authentication API:

| Method | Path | Access | CSRF |
|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Public | Not applicable |
| `POST` | `/api/v1/auth/login` | Public | Required |
| `POST` | `/api/v1/auth/logout` | Public at authorization layer | Required when authenticated |
| `GET` | `/api/v1/auth/me` | Authenticated | Not applicable |

All other application routes remain denied by default. OpenAPI and Swagger UI
remain available in development and disabled under the `prod` profile.

## Architecture decision

The accepted decision is documented in
`docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md`.

Chosen model:

- Spring Security server-side sessions with the HttpOnly `HOSTELOS_SESSION`
  cookie.
- No JWT, no token storage in browser storage, and no stateless fake identity.
- Session identifier rotation on login; server-side invalidation on logout.
- 30-minute idle timeout; `SameSite=Lax`; Secure cookies under `prod`.
- Cookie-backed CSRF token (`XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header).
- CSRF required for login and for authenticated state-changing requests.
- Unauthenticated protected requests retain the existing JSON `401` behavior.

## Identity model and migration

Added forward-only `V2__identity_foundation.sql`.

Created tables:

- `roles`
  - `ADMIN` and `STAFF` seeded as data.
- `user_accounts`
  - Unique normalized login identifier.
  - BCrypt password verifier.
  - `ACTIVE`/`DISABLED` status.
  - Optimistic-lock version and UTC timestamps.
- `user_roles`
  - Composite user/role key with foreign keys and role index.

Login identifiers are normalized with Unicode NFKC, trimming, and lower-casing.
No default administrator account is seeded. No existing data is modified or
deleted by the migration.

## Administrator bootstrap

Bootstrap is disabled by default and runs only when all required values are
provided:

```text
HOSTELOS_BOOTSTRAP_ADMIN_ENABLED=true
HOSTELOS_BOOTSTRAP_ADMIN_LOGIN
HOSTELOS_BOOTSTRAP_ADMIN_DISPLAY_NAME
HOSTELOS_BOOTSTRAP_ADMIN_PASSWORD
```

The password must be at least 16 characters. If the normalized login already
exists, the runner makes no changes. Passwords and password hashes are never
logged. The bootstrap variables should be unset after provisioning.

`Backend/.env.example` now contains blank, disabled placeholders for these
settings. The real `Backend/.env` was not modified.

## Security controls

- Deny-by-default authorization remains in place.
- Every configured authentication route is explicit; no `/api/**` wildcard was
  added.
- Passwords use BCrypt cost 12 and are never returned by the API.
- Login failures are generic and do not reveal whether an account exists.
- Five failures per normalized login plus remote address trigger a generic `429`
  for fifteen minutes.
- The in-memory throttle is bounded to 10,000 keys and prunes expired windows.
- `X-Forwarded-For` is not trusted.
- Disabled accounts cannot authenticate and existing sessions are discarded by
  the active-account filter.
- Roles come from server-side assignments and become `ROLE_ADMIN`/`ROLE_STAFF`.
- Method-security denials now return JSON `403` through
  `GlobalExceptionHandler`; previously they could fall through to a `500`.
- OpenAPI documents the session cookie as the `sessionAuth` security scheme.

## Files added

- `src/main/java/com/rahul/HostelOs/auth/`: controller, request/response DTOs.
- `src/main/java/com/rahul/HostelOs/identity/`: entities, repositories,
  principal, user-details service, normalization, account service, login
  limiter, active-account filter, and bootstrap runner.
- `src/main/resources/db/migration/V2__identity_foundation.sql`
- `src/test/java/com/rahul/HostelOs/AuthIntegrationTests.java`
- `src/test/java/com/rahul/HostelOs/identity/AdminBootstrapRunnerTests.java`
- `src/test/java/com/rahul/HostelOs/identity/LoginIdentifierTests.java`
- `docs/HOSTELOS_MODULE_0.1.3A_REPORT.md`
- `docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md` at the repository root

## Files modified

- `security/SecurityConfig.java`: session auth, CSRF, BCrypt, active filter,
  explicit auth routes, production-aware cookies.
- `config/OpenApiConfig.java`: `sessionAuth` cookie security scheme.
- `common/exception/GlobalExceptionHandler.java`: explicit `403` mapping.
- `application.properties` and `application-prod.properties`: session, CSRF,
  bootstrap, and Secure-cookie settings.
- `Backend/.env.example`: bootstrap-admin placeholders.
- `HostelOsApplicationTests` and `ProductionDocumentationTests`: auth contract,
  production cookie, and documentation assertions.
- `docs/API.md`, `docs/IMPLEMENTATION_LOG.md`, and shared root docs.

No frontend source file was changed.

## Automated verification

Command:

```powershell
$env:JAVA_HOME = 'C:\Users\RAHUL\.jdks\ms-17.0.16'
$env:Path = "C:\Users\RAHUL\.jdks\ms-17.0.16\bin;$env:Path"
.\mvnw.cmd -B clean verify
```

Actual result:

```text
BUILD SUCCESS
Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
Total time: 34.291 s
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar
Jar size: 66,751,865 bytes
```

Test breakdown:

| Test class | Tests | Result |
|---|---:|---|
| `AuthIntegrationTests` | 13 | PASS |
| `ErrorHandlingIntegrationTests` | 2 | PASS |
| `HostelOsApplicationTests` | 12 | PASS |
| `AdminBootstrapRunnerTests` | 4 | PASS |
| `LoginIdentifierTests` | 2 | PASS |
| `ProductionDocumentationTests` | 7 | PASS |
| **Total** | **40** | **PASS** |

The intentional negative-test stack trace is logged by the existing test that
verifies unexpected internal details are sanitized.

## MySQL runtime verification

A fresh temporary database was created on the local MySQL 8.0.40 instance. The
existing `hostelos` database was not modified.

Observed results:

| Check | Result |
|---|---|
| Flyway `V1` and `V2` on an empty MySQL 8 schema | Both applied |
| Hibernate schema validation (`ddl-auto=validate`) | PASS |
| Bootstrap administrator created | PASS |
| Tables | `flyway_schema_history`, `roles`, `user_accounts`, `user_roles` |
| Seeded roles | `ADMIN`, `STAFF` |
| Flyway schema version | `2` |
| `GET /actuator/health` | `200` |
| `GET /api/v1/system/info` | `200` |
| `GET /api/v1/auth/csrf` | `200` |
| `POST /api/v1/auth/login` | `200` |
| `GET /api/v1/auth/me` after login | `200` |
| `POST /api/v1/auth/logout` | `204` |
| `GET /api/v1/auth/me` after logout | `401` |

The temporary verification database was dropped afterward; no verification
databases remain. The local MySQL check is a one-off runtime verification, not
an automated MySQL/Testcontainers suite. Rapid repeated local harness starts
sometimes stalled before Hikari initialization; the recorded final run completed
cleanly and is the evidence retained for this module.

## Repository and secret safety

- Git exists at the repository root; the working branch is
  `feature/0.1.3-identity-authorization`.
- Existing commits were not amended, and no commit was created for this module.
- Tracked env-like filenames are templates/tests only:
  `Backend/.env.example`, `frontend/.env.example`, and `frontend/.env.test`.
- No real `Backend/.env` content was printed or modified.
- No root `.gitignore` exists yet; nested ignores cover the current projects.
- No CI pipeline exists.
- The changes remain uncommitted for review.

## Known limitations

1. No frontend login, logout, CSRF bootstrap, session-expiry handling, or
   authentication UI exists yet.
2. No MFA, password reset, account recovery, email verification, or
   administrative user-management API.
3. No audit trail for login success/failure, logout, role changes, or account
   status changes.
4. The login limiter is in-memory and process-local; a multi-instance deployment
   needs a shared limiter or edge protection.
5. No distributed session store or concurrent-session registry.
6. No absolute session lifetime beyond the 30-minute idle timeout.
7. Role changes are not reflected in an existing session until re-login;
   disablement is checked on each authenticated request.
8. Automated backend tests use H2 MySQL mode; MySQL integration remains a
   manual verification.
9. The local database credential still uses the MySQL `root` account; a
   least-privilege application user remains outstanding.
10. Flyway reports H2 2.4.240 newer than its latest verified version (2.3.232);
    tests pass with the warning.
11. The Maven artifact remains `0.0.1-SNAPSHOT` while product documentation uses
    Module 0.1.3A.
12. No root `.gitignore`, CI, or committed-secret history audit exists.

## Next steps

1. Review and commit the backend changes only after explicit approval.
2. Build the frontend authentication workflow against the documented CSRF,
   login, me, and logout contract.
3. Add CI that runs backend `clean verify` and preserves failing logs.
4. Add a repeatable MySQL integration suite, a least-privilege database user,
   and a root ignore policy.
5. Add audit logging and an administrative account-management workflow before
   using identity in production operations.
