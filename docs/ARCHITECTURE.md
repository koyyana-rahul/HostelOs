# HostelOS Architecture

Last verified: 2026-10-28 (Module 0.1.2 finalization)
Status: one read-only API slice is integrated and technically release-ready; no
business API, authentication, authorization, or multi-tenancy workflow is
implemented. Repository governance (Git/secret audit/CI) remains unresolved.

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
  +-- GET /api/v1/system/info (public, read-only metadata)
  +-- Actuator health (public)
  +-- OpenAPI / Swagger UI (public in development only)
  +-- Flyway
  |
  v
MySQL 8.0 :3306 / database "hostelos"
```

The verified flow is Browser -> relative `/api/v1/system/info` -> Vite proxy ->
Spring Boot controller -> JSON -> RTK Query state -> `/system` page. The
controller does not query the database.

Repository layout:

```text
HostelOs/
  Backend/                  Spring Boot application and Maven wrapper
  frontend/                 React/Vite SPA, tests, and build output
  docs/                     Shared current-state, architecture, and roadmap docs
  .github/                  Currently only a modernization ignore file
```

There is no root build, workspace manifest, Docker Compose file, or Git
repository at this time.

## Frontend

- React 19.3.1 mounted through `createRoot` in strict mode.
- React Router 7.18.5 data router. Routes: `/`, `/foundation`, `/system`, and a
  catch-all not-found route. Pages are lazy-loaded.
- Redux Toolkit 2.13.0 store with a `ui` slice for mobile navigation and the RTK
  Query reducer/middleware.
- RTK Query base API in `src/services/api/baseApi.js`. It reads
  `env.apiBaseUrl`. The first endpoint is injected by
  `src/features/system/api/systemApi.js` as `GET /v1/system/info`, producing
  `/api/v1/system/info` at runtime.
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

## Backend

- Java 17, Spring Boot 4.1.2, Spring Framework 7.0.10, Spring Security 7.1.2.
- Maven wrapper 3.9.9; artifact `com.rahul:HostelOs:0.0.1-SNAPSHOT`.
- Spring Web MVC, Spring Data JPA, Validation, Security, Actuator, Flyway, MySQL
  Connector/J, H2 for tests, and Springdoc OpenAPI 3.2.0.
- Package root is `com.rahul.HostelOs` (existing convention preserved).

Current packages:

```text
com.rahul.HostelOs
  config/OpenApiConfig
  common/exception/ApiError
  common/exception/GlobalExceptionHandler
  security/SecurityConfig
  security/ApiSecurityErrorHandler
  system/SystemInfoController
  system/SystemInfoResponse
```

### Security policy

`SecurityConfig` currently applies a stateless, deny-by-default chain:

- Public: `GET /api/v1/system/info`, `/actuator/health`,
  `/actuator/health/**`, `/v3/api-docs`, `/v3/api-docs/**`,
  `/swagger-ui.html`, `/swagger-ui/**`, `/error`.
- Everything else: `denyAll`, with JSON `401` for unauthenticated requests and
  JSON `403` for denied access.
- CSRF is disabled because the chain is stateless and there is no browser
  session authentication yet.
- No authentication provider or user store exists. The generated default user
  auto-configuration is excluded.
- OpenAPI and Swagger UI are disabled by the `prod` profile.
- Production security tests assert that exactly one `SecurityFilterChain` is
  present and that it is the `SecurityConfig` chain, preventing a probe-only
  test context from silently bypassing production authorization.

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
violations, unexpected exceptions to a generic `500`, and MVC exceptions to the
same shape without leaking internal details.

### Data and migrations

- MySQL URL default:
  `jdbc:mysql://localhost:3306/hostelos?...&serverTimezone=UTC`.
- Hibernate `ddl-auto=validate`; schema changes must go through Flyway.
- `V1__baseline.sql` contains `SELECT 1;`. It establishes version 1 only; there
  are no domain tables.
- `baseline-on-migrate=false`, `clean-disabled=true`,
  `validate-on-migrate=true`, `out-of-order=false`.
- Tests use H2 in MySQL compatibility mode and apply the same migrations.
- No entities or repositories exist yet.

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

`application.properties` imports `.env` with
`spring.config.import=optional:file:.env[.properties]`. Run the backend from the
`Backend/` directory so that relative path resolves. Production must inject the
same variables through the deployment environment or a secret manager. Never
commit a real `.env`; `Backend/.gitignore` excludes `.env` and `.env.*` while
keeping `.env.example`.

### API base path

The agreed dev convention is that the frontend calls `/api/...` and Vite proxies
that prefix to the backend. The first controller is
`GET /api/v1/system/info`, which is public. The response is:

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

The accepted contract is `status: "AVAILABLE"`. The earlier integration brief
expected `"UP"`; that value is retained only as historical context. Every other
unlisted `/api/**` request is rejected by security with JSON `401`. New
controllers should use `/api/v1/...` consistently, and the proxy must not
rewrite away the `/api` prefix.

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

Verified behavior on the development instance:

- `OPTIONS /api/v1/system/info` with an `Origin` header returns `401` and no
  `Access-Control-Allow-Origin`.
- A response to `GET /api/v1/system/info` with an `Origin` header has no
  `Access-Control-Allow-Origin`.
- The Vite proxy works because the browser sees a same-origin `/api` request.
- Direct cross-origin browser access remains unsupported by design.

## Runtime profiles

- Default profile: public health, public OpenAPI/Swagger, deny-all elsewhere.
- `prod` profile: public health, OpenAPI/Swagger disabled (`404`), deny-all
  elsewhere. Verified against MySQL without changing data.
- `test` profile: H2 in-memory database, same Flyway migrations, same production
  security chain. The test-only `/probe/**` routes live in
  `ErrorHandlingIntegrationTests` and are isolated from production-security
  assertions. `HostelOsApplicationTests` asserts that exactly one production
  `SecurityFilterChain` exists.

## Quality gates

```bash
# frontend
npm run lint
npm test
npm run build

# backend (requires JDK 17, Windows Git Bash)
JAVA_HOME="C:/Users/RAHUL/.jdks/ms-17.0.16" \
  cmd //c "mvnw.cmd -B clean verify"
```

Current results: frontend lint clean, 13/13 frontend tests, production build
successful. After the initial 2/9 failure, backend stabilization passed four
clean `19/19` builds, 20 repeated security runs at `17/17`, and five isolated
failing-path runs at `2/2`. The exact historical trigger remains unknown; the
production-chain guard and CI log retention are the residual safeguards. These
gates are not automated in CI, and Git/secret governance remains unresolved.
Technical Module 0.1.2 criteria are satisfied.

## Change rules for the next module

1. Do not weaken `anyRequest().denyAll()`; permit only explicitly reviewed
   routes.
2. Do not add wildcard CORS. Use an explicit origin allowlist and profile
   scoping.
3. Do not commit secrets or real `.env` files.
4. Add Flyway migrations; do not switch to `ddl-auto=update`.
5. Preserve the `ApiError` contract and add tests for new error paths.
6. Keep the frontend API base URL relative (`/api`) for local development and
   document any absolute-URL deployment.
7. Run both quality gates before reporting completion.
8. Record unverified or blocked checks explicitly; do not report transport-only
   checks as a completed API workflow.
9. Keep this slice read-only. Do not add authentication, roles, tenancy, or
   domain tables as part of Module 0.1.2 remediation; Module 0.1.3 requires its
   own approved scope.
10. Preserve the accepted `AVAILABLE` contract and the production
    `SecurityFilterChain` guard. Do not regress either during identity work.
11. Keep CI log retention and the missing-production-chain guard as release
    safeguards if the historical test failure recurs.
