# HostelOS Architecture

Last verified: 2026-10-28 (Modules 0.1.3A-R and 0.1.3B-R role alignment)

Status: the read-only system API remains integrated, and session authentication
and identity are implemented end to end across the backend and frontend. The
global role model now distinguishes platform administration, deferred
organization administration, and staff operations. Business APIs, organization
membership, tenancy, production deployment, CI, and full repository governance
remain outstanding.

## System context

HostelOS is a modular monolith backend with a separate single-page frontend.

```text
Browser
  |
  | http://localhost:5173
  v
Vite dev server (React SPA)
  |
  | /api/* proxy (development only)
  v
Spring Boot backend :8080
  |
  +-- Spring Security (deny by default)
  +-- Session authentication
  |     +-- HOSTELOS_SESSION HttpOnly cookie
  |     +-- XSRF-TOKEN / X-XSRF-TOKEN CSRF
  +-- GET /api/v1/system/info (public, read-only metadata)
  +-- Auth API (/api/v1/auth/*)
  +-- Actuator health (public)
  +-- OpenAPI / Swagger UI (public in development only)
  +-- Flyway
  |
  v
MySQL 8.0.19+ (verified 8.0.40) :3306 / database "hostelos"
```

The verified public flow remains Browser -> relative `/api/v1/system/info` ->
Vite proxy -> Spring Boot controller -> JSON -> RTK Query state -> `/system`
page. The controller does not query the database.

The frontend now calls the authentication API through the same RTK Query client:
it bootstraps `/me`, obtains CSRF tokens before login/logout, protects workspace
routes with session state, renders the current identity/roles, and clears local
state on authenticated `401` responses.

Repository layout:

```text
HostelOs/
  Backend/                  Spring Boot application and Maven wrapper
  frontend/                 React/Vite SPA, tests, and build output
  docs/                     Shared current-state, architecture, and roadmap docs
  .github/                  Repository metadata
```

Git exists at the repository root. The current branch is
`feature/0.1.3-identity-authorization`; Module 0.1.3A/B, 0.1.3A-R, and 0.1.3B-R
changes are uncommitted.
There is no root `.gitignore`, no CI pipeline, and no root build manifest. The
earlier architectural note claiming no Git repository was incorrect.

## Frontend

- React 19.3.1 mounted through `createRoot` in strict mode.
- React Router 7.18.5 data router. Public routes: `/login`; protected routes:
  `/`, `/foundation`, `/system`; plus a protected catch-all not-found route.
  Pages are lazy-loaded.
- Redux Toolkit 2.13.0 store with `ui`, `auth`, and RTK Query state. The `auth`
  slice models `checking`, `authenticated`, `anonymous`, and `unavailable`.
- RTK Query base API in `src/services/api/baseApi.js`. It reads
  `env.apiBaseUrl`, sends cookies, and clears local auth on non-login `401`
  responses. System and auth endpoints are injected into this single client.
- `AuthGate` bootstraps the server session through `GET /api/v1/auth/me`;
  `RequireAuth` redirects anonymous users to `/login` and preserves the intended
  internal destination. The backend remains the authorization boundary.
- `/login` obtains a fresh CSRF token before posting credentials. Logout does the
  same before invalidating the session. No password or token is written to
  browser storage; the HttpOnly `HOSTELOS_SESSION` cookie is the credential.
- The sidebar displays the safe `/me` identity and the exact server-assigned
  `PLATFORM_ADMIN`/`ORGANIZATION_ADMIN`/`STAFF` labels as informational
  metadata. No route, navigation, action, hierarchy, or administration screen is
  derived from a role label. Focus and visibility changes trigger revalidation
  without keep-alive polling.
- The `/system` page validates `applicationName`, `apiVersion`, and `status`
  before rendering and shows loading, error, retry, and success states.
- Tailwind CSS 4 through the Vite plugin. Design tokens live in
  `src/styles/globals.css`.
- Vitest 5 with jsdom, React Testing Library, and jest-dom.
- Environment access is centralized in `src/lib/env.js`.

Frontend route and API conventions for new work:

1. Add a feature under `src/features/<capability>/`.
2. Inject endpoints into `baseApi` rather than creating a second API client.
3. Register routes in `src/app/routes.js` and navigation in
   `src/components/layout/navigation.js`.
4. Add tests under `tests/`, using `renderApp` from `tests/test-utils.jsx`.
5. Keep browser-visible configuration prefixed with `VITE_`.

Authentication is a frontend UX boundary only. The API still requires the
session cookie, CSRF header on mutating requests, and server-side authorization.
Login errors are generic; invalid credentials and disabled accounts are not
distinguished. Real browser flows from login through session restore and logout
have been exercised through the Vite proxy, including the final V3
`PLATFORM_ADMIN` role contract.

## Backend

- Java 17, Spring Boot 4.1.2, Spring Framework 7.0.10, Spring Security 7.1.2.
- Maven wrapper 3.9.9; artifact `com.rahul:HostelOs:0.0.1-SNAPSHOT`.
- Spring Web MVC, Spring Data JPA, Validation, Security, Actuator, Flyway, MySQL
  Connector/J, H2 for tests, and Springdoc OpenAPI 3.2.0.
- Package root is `com.rahul.HostelOs` (existing convention preserved).

Current backend packages:

```text
com.rahul.HostelOs
  auth/
    AuthController
    AuthUserResponse
    CsrfResponse
    LoginRequest
  common/exception/
    ApiError
    GlobalExceptionHandler
  config/OpenApiConfig
  identity/
    AccountStatus, Role, RoleName
    UserAccount, UserAccountRepository
    UserAccountService
    HostelOsPrincipal, HostelOsUserDetailsService
    LoginIdentifier
    LoginAttemptService
    ActiveAccountFilter
    AdminBootstrapProperties, AdminBootstrapRunner
    RoleRepository, DuplicateLoginIdentifierException
  security/
    SecurityConfig
    ApiSecurityErrorHandler
  system/
    SystemInfoController
    SystemInfoResponse
```

### Authentication architecture

The accepted decision is recorded in
`docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md`.

- Spring Security server-side sessions, no JWT.
- Session cookie: `HOSTELOS_SESSION`, HttpOnly, `SameSite=Lax`, 30-minute idle
  timeout; Secure under `prod`.
- Session identifier rotated after login.
- Logout invalidates the server session.
- CSRF: `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` header. The CSRF cookie is not
  HttpOnly because the same-origin SPA must read it.
- CSRF is required for login and authenticated state-changing requests.
  Unauthenticated protected requests remain JSON `401`.
- Password hashing: BCrypt cost 12.
- Roles: server-side `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF`,
  returned by the identity API under those bare names. Spring enforcement
  derives internal `ROLE_PLATFORM_ADMIN`, `ROLE_ORGANIZATION_ADMIN`, and
  `ROLE_STAFF` authorities; the prefixed forms are not API values.
- No implicit role hierarchy exists. `PLATFORM_ADMIN` is platform-wide;
  `ORGANIZATION_ADMIN` is reserved for future organization-scoped work and never
  authorizes platform endpoints; `STAFF` covers operational workflows.
- The V3 migration renames the original `ADMIN` role to `PLATFORM_ADMIN` in
  place, preserving role IDs and `user_roles` assignments. Existing `ADMIN`
  accounts therefore retain their original platform-wide meaning.
- Login normalization: Unicode NFKC, trim, lower-case.
- Login limiter: 5 failures per 15 minutes, keyed by normalized login plus
  remote address, bounded in-memory state; `X-Forwarded-For` is not trusted.
- Disabled accounts cannot authenticate; active sessions are discarded by the
  active-account filter when status is no longer `ACTIVE`.
- No default account is seeded. The optional bootstrap runner creates one
  `PLATFORM_ADMIN` only when `HOSTELOS_BOOTSTRAP_ADMIN_ENABLED=true` and all
  required bootstrap values are supplied.

Auth API:

| Method | Path | Access | CSRF |
|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Public | Not applicable |
| `POST` | `/api/v1/auth/login` | Public | Required |
| `POST` | `/api/v1/auth/logout` | Public at authorization layer | Required when authenticated |
| `GET` | `/api/v1/auth/me` | Authenticated | Not applicable |

### Authorization policy

Application routes are explicitly reviewed; there is no `/api/**` wildcard:

- Public: `GET /api/v1/system/info`, `GET /api/v1/auth/csrf`,
  `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`.
- Authenticated: `GET /api/v1/auth/me`.
- All other application routes: `denyAll`, with JSON `401` for unauthenticated
  requests and JSON `403` for denied access.

Infrastructure routes remain public according to the existing policy:
`/actuator/health`, `/actuator/health/**`, `/v3/api-docs`, `/v3/api-docs/**`,
`/swagger-ui.html`, `/swagger-ui/**`, and `/error`.

OpenAPI and Swagger UI are disabled by the `prod` profile. OpenAPI documents the
session cookie as the `sessionAuth` security scheme.

Production security tests still assert that the production
`SecurityFilterChain` is present and preserved; test-only probe chains remain
isolated.

### Error contract

`ApiError` is the shared JSON error shape:

```json
{
  "timestamp": "2026-10-28T13:05:00.366140300Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required",
  "path": "/api/not-implemented",
  "violations": []
}
```

`GlobalExceptionHandler` maps validation failures to `400` with field
violations, method-security denials to `403`, unexpected exceptions to a generic
`500`, and MVC exceptions to the same shape without leaking internal details.

### Data and migrations

- MySQL URL default:
  `jdbc:mysql://localhost:3306/hostelos?...&serverTimezone=UTC`.
- Hibernate `ddl-auto=validate`; schema changes must go through Flyway.
- `V1__baseline.sql` establishes version 1 only.
- `V2__identity_foundation.sql` adds `roles`, `user_accounts`, and `user_roles`,
  with the original role seed data and status/login/password constraints.
- `V3__reconcile_platform_admin_role.sql` renames `ADMIN` to `PLATFORM_ADMIN` in
  place, seeds `ORGANIZATION_ADMIN`, updates descriptions, and replaces the
  role-name check constraint. Role IDs and assignments are preserved.
- `baseline-on-migrate=false`, `clean-disabled=true`,
  `validate-on-migrate=true`, `out-of-order=false`.
- Tests use H2 in MySQL compatibility mode and apply the same migrations.
  `RoleMigrationTests` explicitly upgrades a V2 database containing identity and
  role-assignment rows to V3.
- Fresh MySQL 8.0.40 V1+V2+V3 and V2-to-V3 upgrade paths were verified on
  disposable schemas, including Hibernate validation and bootstrap; the
  temporary schemas were dropped.

### MySQL compatibility

Decision recorded 2026-10-30.

- Minimum supported server: MySQL 8.0.19+. V3 uses
  `ALTER TABLE roles DROP CONSTRAINT chk_roles_name`, which requires 8.0.19+.
  MySQL 8.0.16-8.0.18 supports only `DROP CHECK`, while earlier 8.0 releases
  parsed and ignored `CHECK` constraints.
- The documented verified target is MySQL 8.0.40. V3 remains unchanged because
  it is published Flyway history; it is valid on MySQL 8.0.19+ and H2.
- H2 2.4.240 in MySQL mode supports `DROP CONSTRAINT`, not `DROP CHECK`. The
  existing H2 test profile remains unchanged.
- H2 compatibility-mode tests exercise migration sequencing and SQL behavior
  only. They do not establish full MySQL/InnoDB compatibility, including
  storage-engine, locking, collation, index, and optimizer behavior.
- Live MySQL verification for the current V1-V5 working tree is still pending.
  The earlier MySQL 8.0.40 evidence covers V1-V3 only and used disposable
  schemas that were dropped.

## Configuration conventions

### Frontend (`frontend/`)

| Variable | Default | Purpose |
|---|---|---|
| `VITE_APP_NAME` | `HostelOS` | Browser-visible application name |
| `VITE_APP_VERSION` | `0.1.1` | Browser-visible version label |
| `VITE_API_BASE_URL` | `/api` | RTK Query base URL |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Vite dev proxy target |

Only `VITE_`-prefixed values reach the browser. They are public values, not
secrets. Local overrides belong in `.env.local`; `.env` and `.env.*.local` are
ignored.

### Backend (`Backend/`)

| Variable | Default | Purpose |
|---|---|---|
| `HOSTELOS_DB_URL` | Local `hostelos` MySQL URL | JDBC URL |
| `HOSTELOS_DB_USERNAME` | none | Database user |
| `HOSTELOS_DB_PASSWORD` | none | Database password |
| `HOSTELOS_DB_POOL_SIZE` | `10` | Hikari maximum pool size |
| `HOSTELOS_SERVER_PORT` | `8080` | HTTP port |
| `HOSTELOS_BOOTSTRAP_ADMIN_ENABLED` | `false` | Enable one-time admin bootstrap |
| `HOSTELOS_BOOTSTRAP_ADMIN_LOGIN` | none | Bootstrap admin login |
| `HOSTELOS_BOOTSTRAP_ADMIN_DISPLAY_NAME` | none | Bootstrap admin display name |
| `HOSTELOS_BOOTSTRAP_ADMIN_PASSWORD` | none | Bootstrap admin password, minimum 16 characters |

`application.properties` imports `.env` with
`spring.config.import=optional:file:.env[.properties]`. Run the backend from the
`Backend/` directory so that relative path resolves. Production must inject the
same variables through the deployment environment or a secret manager. Never
commit a real `.env`; `Backend/.gitignore` excludes `.env` and `.env.*` while
keeping `.env.example`.

Bootstrap should be disabled or unset after provisioning. The current local
database credential still uses the MySQL `root` account; creating a
least-privilege application user remains outstanding.

### API base path

The frontend calls `/api/...` and Vite proxies that prefix to the backend. The
accepted public endpoint is `GET /api/v1/system/info`; its response is:

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

The accepted contract is `status: "AVAILABLE"`. New controllers should use
`/api/v1/...` consistently, and the proxy must not rewrite away the `/api`
prefix.

## CORS policy

No CORS configuration exists in either profile, intentionally. The decision is:

- **Development:** use the same-origin Vite `/api` proxy. The browser never
  makes a cross-origin request, so no CORS headers are needed.
- **Initial production:** prefer a same-origin reverse proxy (or equivalent)
  that serves the SPA and forwards `/api` to Spring Boot. This keeps the browser
  same-origin and avoids a CORS policy in the first release.
- **Future separate origins:** add an explicit, profile-scoped origin allowlist,
  drive production origins from configuration, and test allowed-origin,
  rejected-origin, and preflight behavior. Never use `*` with credentials.

A response to `GET /api/v1/system/info` with an `Origin` header has no
`Access-Control-Allow-Origin`. The Vite proxy works because the browser sees a
same-origin `/api` request. Direct cross-origin browser access remains
unsupported by design.

## Runtime profiles

- Default profile: public health, public OpenAPI/Swagger, system info and auth
  routes per policy; deny-all elsewhere.
- `prod` profile: public health, OpenAPI/Swagger disabled (`404`), Secure session
  and CSRF cookies; deny-all elsewhere.
- `test` profile: H2 in-memory database, same Flyway migrations, same production
  security chain. Test-only probe routes are isolated in
  `ErrorHandlingIntegrationTests`. `HostelOsApplicationTests` asserts that the
  production `SecurityFilterChain` is present.
- `AuthIntegrationTests` adds a test-only security chain scoped to `/_test/**`
  with `@PreAuthorize` probes verifying that platform, organization, and staff
  authorities are distinct.
- `RoleMigrationTests` exercises fresh role seeding and a V2-to-V3 upgrade with
  existing user and role-assignment rows.

## Quality gates

```bash
# frontend
npm run lint
npm test
npm run build

# backend (requires JDK 17, PowerShell)
$env:JAVA_HOME = 'C:\Users\RAHUL\.jdks\ms-17.0.16'
$env:Path = "C:\Users\RAHUL\.jdks\ms-17.0.16\bin;$env:Path"
.\mvnw.cmd -B clean verify
```

Current results:

- Backend clean verify: 44/44 tests, `BUILD SUCCESS`.
- MySQL 8.0.40 (historical, V1-V3 only): fresh V1+V2+V3 and V2-to-V3 upgrade
  paths, Hibernate validation, a `PLATFORM_ADMIN` bootstrap, and the auth flow
  verified on disposable schemas; those schemas were dropped afterward. This
  evidence does not cover V4-V5.
- Frontend: lint clean, 37/37 tests in 8 files, and a successful production
  build; role-contract regressions use the finalized three-role names. A
  final-contract live run through the Vite proxy against the V3 jar confirmed a
  `PLATFORM_ADMIN` identity, session restore, and logout. A separate role-label
  visual check passed at desktop and mobile widths.
- No CI automation; all gates and browser checks are manual.

## Change rules for the next module

1. Preserve deny-by-default authorization and explicit route review.
2. Do not weaken session, CSRF, cookie, or account-status enforcement.
3. Do not add wildcard CORS. Use an explicit origin allowlist and profile
   scoping.
4. Do not commit secrets or real `.env` files.
5. Add Flyway migrations; do not switch to `ddl-auto=update`.
6. Preserve the `ApiError` contract and add tests for new error paths.
7. Keep the frontend API base URL relative (`/api`) for local development and
   document any absolute-URL deployment.
8. Run both quality gates before reporting completion; record blocked checks.
9. Keep the frontend auth workflow aligned with the documented CSRF/session
   contract; do not treat client route guards as authorization.
10. Add CI, checked-in browser auth coverage, and a repeatable MySQL integration
    suite before production use.
11. Resolve the local `root` database credential and add a root ignore/secret
    policy.
12. Keep role assignments server-controlled. Do not accept roles from login or
    other untrusted input, and do not treat `ORGANIZATION_ADMIN` as a platform
    administrator or claim tenant isolation before a tenant model exists.
