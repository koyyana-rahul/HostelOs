# HostelOS Current State

Last verified: 2026-10-28 (Modules 0.1.3A-R and 0.1.3B-R role alignment)

Verified by: frontend lint/tests/build, backend clean verify, package and source
fact checks, a role-label visual check, and real browser login/logout flows
through fresh MySQL 8 schemas, including the final V3 role contract

## Summary

HostelOS has one public read-only API workflow and an end-to-end server-managed
identity and authentication foundation across the backend and frontend.

The existing `/system` page still calls `GET /api/v1/system/info` through the
shared RTK Query client. That contract is unchanged:

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

Module 0.1.3A adds:

- Server-side Spring Security sessions with the HttpOnly `HOSTELOS_SESSION`
  cookie.
- CSRF bootstrap and validation with `XSRF-TOKEN` plus `X-XSRF-TOKEN`.
- Login, logout, and current-identity endpoints.
- Persisted users, roles, and account status in Flyway `V2`.
- BCrypt password hashing, generic login failures, and bounded login throttling.
- An optional environment-driven one-time administrator bootstrap.

Module 0.1.3A-R adds:

- Distinct `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF` roles.
- A forward-only Flyway `V3` migration that renames the original `ADMIN` role
  to `PLATFORM_ADMIN` in place, preserves all users and assignments, and seeds
  `ORGANIZATION_ADMIN`.
- A bootstrap administrator that receives only `PLATFORM_ADMIN`, and only when
  the explicit bootstrap configuration is enabled.
- Authorization-boundary tests proving an organization administrator is not a
  platform administrator and that roles cannot be supplied by a login request.

`ORGANIZATION_ADMIN` is a role definition only. Organization membership, tenant
boundaries, and organization-scoped enforcement remain deferred.

Module 0.1.3B adds:

- A public `/login` route and a responsive credential form.
- `AuthGate` session bootstrap through `GET /api/v1/auth/me`, with retryable
  unavailable-backend handling.
- `RequireAuth` protected routes that preserve the requested internal path.
- CSRF bootstrap immediately before login and logout.
- Cookie-backed RTK Query requests with global `401` session clearing.
- Sidebar identity, server-assigned role labels, and sign-out. Role labels are
  informational and do not control navigation or actions.
- Focus-based session revalidation and safe auth error messages.
- No password, session token, CSRF token, or access token in browser storage.

Module 0.1.3B-R adds:

- Frontend fixtures, regression tests, and documentation aligned with
  `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF`.
- Sidebar rendering of the exact server role names with an explicit note that
  access is enforced by the backend.
- No role hierarchy, admin screen, or client-side permission derived from a
  label; `ORGANIZATION_ADMIN` remains a role definition only.
- Frontend lint clean, 37/37 tests in 8 files, successful production build, and
  exact role-label rendering at desktop and mobile widths.
- A real-browser run against the V3 backend confirmed `/me` returns
  `PLATFORM_ADMIN`, the sidebar renders only that role, and no administration
  navigation appears.

No administrative user-management, MFA, password reset, account recovery, audit
trail, or CI pipeline exists yet.

## Release status

| Gate | Status | Detail |
|---|---|---|
| Module 0.1.2 | **PASS** | Read-only system-info integration remains intact |
| Module 0.1.3A backend | **PASS** | Identity/auth backend complete; reconciled by 0.1.3A-R |
| Module 0.1.3A-R roles | **PASS** | Three-role model implemented; `clean verify` 44/44 |
| Module 0.1.3B frontend | **PASS** | Session/login/logout integration complete; 37/37 tests, lint and build clean |
| Module 0.1.3B-R role alignment | **PASS** | Frontend fixtures, display, tests, and docs aligned with the three-role contract |
| Module 0.1.4 Phase B backend | **PASS** | Organization & membership database foundation; Flyway V4, entities, repositories, 75/75 backend tests passing |
| MySQL 8 runtime | **PASS** | Fresh V1+V2+V3 schema and V2-to-V3 upgrade verified on disposable schemas |
| Frontend auth integration | **PASS** | Real browser login, final `PLATFORM_ADMIN` identity, session restore, logout, and post-logout `401` verified |
| Repository governance | **RESOLVED** | Git exists at HostelOS root on branch `feature/0.1.3-identity-authorization`; root `.gitignore` added; CI workflows implemented (see CI gates below) |
| Production deployment | **DEFERRED** | Same-origin proxy/CORS topology and operational hardening remain outstanding |

## Verified status

| Area | Status | Evidence |
|---|---|---|
| Backend clean verify | PASS | `BUILD SUCCESS`, 44/44 tests |
| Auth integration tests | PASS | 15/15 session, CSRF, limiter, bootstrap, seeded-role, and role-boundary tests |
| Role migration tests | PASS | 2/2 fresh-schema and V2-to-V3 upgrade compatibility tests |
| Bootstrap unit tests | PASS | 4/4 |
| Identifier normalization tests | PASS | 2/2 |
| Existing backend tests | PASS | Error handling 2/2, application 12/12, production docs 7/7 |
| JAR packaging | PASS | `target/HostelOs-0.0.1-SNAPSHOT.jar` |
| MySQL migration | PASS | Flyway V1+V2+V3 on fresh and V2-to-V3 upgrade schemas |
| Hibernate schema validation | PASS | `ddl-auto=validate` against MySQL 8 |
| Bootstrap administrator | PASS | Created once with `PLATFORM_ADMIN` only |
| Auth runtime | PASS | CSRF `200`, login `200`, me `200`, logout `204`, me after logout `401` |
| Browser auth flow | PASS | Login, protected shell, final `PLATFORM_ADMIN` identity, `/system` reload, logout, and session invalidation verified |
| Role contract | PASS | API server roles are `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF`; no implicit hierarchy; organization admin denied platform access |
| Role input boundary | PASS | Extra `roles` field on login is ignored; authorities remain database-derived |
| Frontend role metadata | PASS | Fixtures and tests use the final three names; labels are display-only and navigation is role-independent |
| Browser storage | PASS | Zero `localStorage`/`sessionStorage` entries after login and logout |
| Login throttling | PASS | 5 failures/15 minutes, bounded in-memory state, generic `429` |
| CSRF policy | PASS | Required for login and authenticated mutations; unauthenticated protected requests remain `401` |
| Session policy | PASS | HttpOnly, `SameSite=Lax`, 30-minute idle timeout, rotation on login |
| Production cookies | PASS | Secure session and CSRF cookies under `prod` |
| System API contract | PASS | `HostelOS`, `v1`, `AVAILABLE` unchanged |
| OpenAPI security scheme | PASS | `sessionAuth` cookie scheme documented |
| Development proxy / CORS | UNCHANGED | Vite proxy remains the only supported development topology |
| Frontend gates | PASS | Lint clean, 37/37 tests in 8 files, successful production build |
| Frontend role visual check | PASS | Exact three-role rendering at 1440x900 and 390x844 with no overflow or console errors |
| Frontend live integration | PASS | Final-contract run: real browser -> Vite proxy -> 0.1.3A-R jar -> disposable MySQL 8 V3 schema |
| Git diff / change preservation | PARTIAL | Branch and working-tree changes visible; Module 0.1.3A/B, 0.1.3A-R, and 0.1.3B-R changes uncommitted |
| Committed-secret verification | PARTIAL | Filename audit found templates/tests only; no full history scan or CI gate. CI workflows added in `.github/workflows/` provide automated backend and frontend quality gates. |
| CI gates | IMPLEMENTED | Backend: `mvnw -B clean verify`; Frontend: `npm ci`, `npm run lint`, `npm test -- --run`, `npm run build`. Workflows defined in `.github/workflows/backend-ci.yml` and `.github/workflows/frontend-ci.yml`. |

## Authentication behavior

| Method | Path | Access | CSRF |
|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Public | Not applicable |
| `POST` | `/api/v1/auth/login` | Public | Required |
| `POST` | `/api/v1/auth/logout` | Public at authorization layer | Required when authenticated |
| `GET` | `/api/v1/auth/me` | Authenticated | Not applicable |

All other application routes remain denied by default. Infrastructure routes
such as health, OpenAPI, Swagger UI, and `/error` retain their existing profile
policy.

Login identifiers are normalized with Unicode NFKC, trimming, and lower-casing.
Passwords are BCrypt cost 12. Authentication failures are generic. Disabled
accounts cannot authenticate, and authenticated requests re-check account status.
Roles are server-side `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, or `STAFF`
assignments; role changes take effect on the next login. The identity API returns
those bare names; prefixed `ROLE_*` values are internal Spring Security
authorities. There is no implicit hierarchy: an organization administrator is
not a platform administrator, and neither role grants the other's endpoints.
The frontend displays the server-provided role labels as informational metadata
and does not derive permissions, routes, or actions from them. Its fixtures,
tests, and documentation use the same three finalized names; no role hierarchy
or administration capability is inferred.

The frontend bootstraps the session through `/me`, redirects anonymous users to
`/login`, and preserves the intended internal destination. Its route guards are
navigation UX only; every API request is still authorized by the backend. Login,
logout, and session clearing keep passwords and tokens out of browser storage.

The administrator bootstrap is disabled by default and requires all four
`HOSTELOS_BOOTSTRAP_ADMIN_*` values. When enabled, it creates only a
`PLATFORM_ADMIN` account. The password minimum is 16 characters. No default
account is seeded. `Backend/.env.example` contains disabled, blank placeholders;
the real `Backend/.env` was not modified.

## Data and migration

`V2__identity_foundation.sql` adds:

- `roles` with the original `ADMIN` and `STAFF` seed data.
- `user_accounts` with unique normalized login, display name, BCrypt hash,
  status, optimistic-lock version, and UTC timestamps.
- `user_roles` with foreign keys and a role index.

`V3__reconcile_platform_admin_role.sql`:

- Drops the old role-name check constraint.
- Renames `ADMIN` to `PLATFORM_ADMIN` in place, preserving role IDs and all
  `user_roles` assignments.
- Inserts `ORGANIZATION_ADMIN` with no user assignments and updates role
  descriptions.
- Installs a check constraint allowing exactly `PLATFORM_ADMIN`,
  `ORGANIZATION_ADMIN`, and `STAFF`.

No users, assignments, or role rows are deleted. The migrations are additive and
forward-only. Do not use Flyway clean/reset, schema resets, or `ddl-auto=update`.

MySQL verification used disposable temporary schemas. One applied V1+V2+V3 from
empty; another applied V1+V2, inserted an administrator with the old `ADMIN`
assignment, then applied V3 and confirmed the assignment became
`PLATFORM_ADMIN`. Both temporary schemas were dropped. The existing `hostelos`
database was not modified.

## Repository state

- Git exists at the HostelOS root.
- Current branch: `feature/0.1.3-identity-authorization`.
- Existing commits: `577a2b2 chore: establish HostelOS foundation`, `8f7b72b
  first commit`.
- Module 0.1.3A backend, Module 0.1.3A-R role reconciliation, Module 0.1.3B
  frontend, and 0.1.3B-R role-alignment changes are uncommitted.
- No root `.gitignore` exists. Nested Backend/frontend ignores cover the current
  projects.
- Tracked env-like filenames are `Backend/.env.example`,
  `frontend/.env.example`, and `frontend/.env.test`; no real `.env` file is
  tracked by filename audit.
- No CI pipeline exists.
- Earlier documentation claiming no Git repository was incorrect and has been
  corrected.

## Environment used

| Component | Value |
|---|---|
| OS | Windows |
| JDK | Microsoft OpenJDK 17.0.16 |
| Maven | Wrapper 3.9.9 |
| Spring Boot | 4.1.2 |
| Spring Security | 7.1.2 |
| MySQL | 8.0.40, service `MySQL80`, port 3306 |
| Automated backend database | H2 2.4.240 in MySQL compatibility mode |
| Frontend | React 19.3.1, Vite 8.3.5, Redux Toolkit 2.13.0, Vitest 5.0.5 |
| Frontend commands | `npm run lint`, `npm test`, `npm run build` |
| Backend build command | `.\mvnw.cmd -B clean verify` with JDK 17 |

## Outstanding issues

1. **No administrative user-management API.** Bootstrap is the only
   account-creation path.
2. **No MFA, password reset, account recovery, or email verification.**
3. **No audit trail.** Login success/failure, logout, role changes, and status
   changes are not recorded.
4. **Limiter is process-local.** A multi-instance deployment needs shared state
   or edge protection.
5. **No distributed sessions or concurrent-session control.**
6. **Local MySQL uses `root`.** A least-privilege application user remains a
   follow-up.
7. **No repeatable MySQL integration suite.** Automated tests use H2; the MySQL
   checks were runtime verifications.
8. **No CI.** Gates, browser coverage, and log retention are manual.
9. **No checked-in browser auth automation.** The complete-flow check was a
   one-off verification against a temporary schema.
10. **Maven artifact version drift.** `pom.xml` remains `0.0.1-SNAPSHOT` while
     product documentation uses module versions.
11. **H2/Flyway warning.** Flyway reports H2 2.4.240 newer than its verified
     2.3.232; tests pass with warning.
12. **Startup directory dependency.** `spring.config.import=optional:file:.env`
     resolves relative to the process working directory. Start the backend from
     `Backend/` or inject environment variables externally.
13. **Production topology deferred.** Same-origin reverse proxy, secure-origin
     validation, and production session storage remain deployment work.
14. **Invalid session cookie retention.** Logout invalidates the server session,
     but the HttpOnly cookie can remain in the browser until it is discarded;
     subsequent `/me` requests return `401`.
15. **Organization authorization is not implemented.** `ORGANIZATION_ADMIN`
     exists but organization membership, tenant boundaries, and organization
     APIs do not; assigning it grants no organization-scoped access.
16. **Existing `ADMIN` accounts become platform administrators.** The V3 rename
     preserves the original platform-wide meaning. Operators must review
     privileged accounts before applying the migration.
17. **Stale session authorities.** An authenticated session keeps its old role
     names until re-login after the V3 migration.
18. **Process-local login throttling.** 5 failures per 15 minutes, bounded in-memory
     state; multi-instance deployment needs shared limiter or edge protection.
19. **No distributed sessions or concurrent-session control.** Single-instance only;
     horizontal scaling requires sticky sessions or a shared session store.
20. **No repeatable MySQL integration suite.** Automated tests use H2; MySQL checks
     were runtime verifications on disposable schemas.

## References

- `docs/MODULE_0.1.3B_FRONTEND_REPORT.md` - frontend implementation and evidence.
- `frontend/README.md` - frontend setup, auth flow, and limitations.
- `docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md` - accepted identity/auth design.
- `Backend/docs/HOSTELOS_MODULE_0.1.3A_REPORT.md` - backend implementation and evidence.
- `Backend/docs/HOSTELOS_MODULE_0.1.3A-R_ROLE_RECONCILIATION_REPORT.md` - platform-role reconciliation evidence.
- `Backend/docs/API.md` - current API contract.
- `docs/ARCHITECTURE.md` - current architecture and boundaries.
- `docs/ROADMAP.md` - module sequence and open prerequisites.
- `docs/IMPLEMENTATION_LOG.md` - shared implementation history.
- `docs/INTEGRATION_QA_0.1.2.md` - historical Module 0.1.2 QA evidence.
