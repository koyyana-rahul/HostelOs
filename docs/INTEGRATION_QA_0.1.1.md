# HostelOS Foundation Integration and QA Report

> Historical report: this describes the foundation before Module 0.1.2B. For
> the current `/api/v1/system/info` integration and release-gate result, see
> `docs/INTEGRATION_QA_0.1.2.md`, `docs/MODULE_0.1.2B_FRONTEND_REPORT.md`, and
> `docs/CURRENT_STATE.md`.

Module: 0.1.1 Foundation
Date: 2026-10-28
Scope: existing frontend and backend work in `D:\Downloads\HostelOs`
Approval: no production deployment, no destructive database change, no commit

## Executive result

The frontend and backend foundations are individually sound and mutually
consistent at the transport level:

- Frontend: lint clean, 4/4 tests passed, production build passed, all routes
  rendered in a headless browser with no console errors.
- Backend: clean build passed, 7/7 tests passed, packaged application started
  against MySQL 8.0.40, Flyway validated version 1, and health returned `UP`.
- Development proxy: `/api` requests from Vite reached the backend on the default
  port and returned the backend's JSON error response.
- Production profile: health `200`, OpenAPI and Swagger UI `404`, other routes
  `401`.

The system is **not fully integrated**. There is no business endpoint, no
authentication, and no frontend API workflow using real data. Direct
cross-origin browser calls fail because CORS is not configured. No Git
repository exists, so diff review, tracked-secret verification, and commit are
blocked.

## Task-by-task results

| # | Task | Result | Evidence |
|---|---|---|---|
| 1 | Review Git diff and preserve existing changes | BLOCKED | No `.git` directory; `git rev-parse` returned `fatal: not a git repository`. No source or config was edited during QA. |
| 2 | Verify frontend/backend dependency compatibility | PASS | `npm ls --depth=0` clean; Maven dependency tree resolved; frontend build/test and backend build/test/runtime all succeeded |
| 3 | Verify startup configuration | PASS with caveat | Backend started on default 8080 and isolated 8081/8082; Vite started on 5175/5176; default proxy target 8080 worked. Backend `.env` import depends on running from `Backend/`. |
| 4 | Verify API base URL and environment conventions | PASS with gap | `VITE_API_BASE_URL=/api`, proxy target `http://localhost:8080`, backend port 8080. No `/api` endpoint exists yet. |
| 5 | Check CORS for local development | FAIL (gap, safe default) | No CORS config; preflight `401`; no `Access-Control-Allow-Origin`; direct browser `fetch` failed. Vite proxy works. |
| 6 | Check for committed secrets | BLOCKED (partial scan) | No Git. Local `.env` is ignored and contains the DB credentials; password not found outside `.env`; `.env.example` uses placeholders only. |
| 7 | Run frontend build and tests | PASS | See frontend evidence below |
| 8 | Run backend build and tests | PASS | See backend evidence below |
| 9 | Verify health and basic rendering | PASS | Health `200` with `UP`; SPA routes and mobile navigation rendered correctly |
| 10 | Identify duplicate/conflicting config and missing dependencies | PASS (findings) | No missing dependencies; version drift and config duplication documented below |
| 11 | Do not invent integration results | HONORED | Report distinguishes transport verification from a connected API workflow |
| 12 | Update current state, roadmap, architecture docs | DONE | `docs/CURRENT_STATE.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`, this report |
| 13 | Produce combined report | DONE | This document |

## Frontend evidence

Environment: Node.js 22.14.0, npm 10.8.1, Vite 8.3.5, Vitest 5.0.5.

```text
$ npm run lint
> eslint .
(no errors or warnings)
```

```text
$ npm test
> vitest run

 Test Files  2 passed (2)
      Tests  4 passed (4)
   Duration  3.47s
```

```text
$ npm run build
> vite build
vite v8.3.5 building client environment for production...
✓ 1999 modules transformed.
dist/index.html                             0.67 kB
dist/assets/index-DQTUFyf8.css             26.64 kB
dist/assets/NotFoundPage-C3vNTNP7.js        1.26 kB
dist/assets/DashboardPage-DcVIijzi.js       6.45 kB
dist/assets/FoundationPage-HFD2P85R.js      8.31 kB
dist/assets/index-C4ZkLnD_.js             403.49 kB
✓ built in 790ms
```

Dependency check:

```text
$ npm ls --depth=0
hostelos-frontend@0.1.1
├── react@19.3.1
├── react-dom@19.3.1
├── react-router-dom@7.18.5
├── @reduxjs/toolkit@2.13.0
├── react-redux@9.3.1
├── vite@8.3.5
├── vitest@5.0.5
├── eslint@10.13.0
└── (all other declared packages resolved)
```

No unmet peer dependencies or invalid packages were reported.

Browser verification of the production build (`vite preview`):

```text
http 200, title "Overview · HostelOS", heading "Welcome to HostelOS"
nav links: 2
API base text: /api
/foundation: title "Foundation · HostelOS", heading "A clear base for every module"
/missing-page: title "Page not found · HostelOS", heading "Page not found"
390px viewport: mobile navigation dialog visible, aria-expanded="true"
console errors/warnings: 0
failed requests: 0
```

A desktop screenshot was inspected: sidebar, header, status cards, checklist,
API panel, commands, and footer all rendered without clipping, overlap, or blank
regions.

## Backend evidence

Environment: Microsoft OpenJDK 17.0.16, Maven wrapper 3.9.9, Spring Boot 4.1.2.

```text
$ JAVA_HOME="C:/Users/RAHUL/.jdks/ms-17.0.16" ./mvnw -B clean verify

[INFO] Running com.rahul.HostelOs.HostelOsApplicationTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.168 s
[INFO] BUILD SUCCESS
[INFO] Total time: 18.794 s
```

The tests cover:

1. Spring context startup.
2. Public sanitized health endpoint.
3. Public OpenAPI document.
4. Public Swagger UI.
5. JSON `401` for unlisted endpoints.
6. Validation failure shape.
7. Generic `500` without internal detail leakage.

Resolved dependency versions include:

| Component | Version |
|---|---|
| Spring Boot | 4.1.2 |
| Spring Framework / Web MVC | 7.0.10 |
| Spring Security | 7.1.2 |
| Hibernate ORM | 7.4.13.Final |
| Tomcat | 11.0.27 |
| Flyway | 12.4.0 |
| MySQL Connector/J | 9.7.0 |
| Springdoc OpenAPI | 3.2.0 |
| JUnit Jupiter | 6.0.3 |
| H2 (test) | 2.4.240 |

Flyway logs a warning that H2 2.4.240 is newer than its verified 2.3.232. This
did not fail any test.

## Runtime evidence

Development/default stack:

```text
MySQL 8.0.40 on localhost:3306
Backend default port 8080 -> /actuator/health 200
Vite dev server -> /api/not-implemented proxied to backend
Backend response through proxy:
  HTTP 401 application/json
  {"status":401,"error":"Unauthorized","message":"Authentication is required",
   "path":"/api/not-implemented","violations":[]}
```

Health response:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

Development endpoints:

```text
GET /actuator/health   200
GET /v3/api-docs       200, contains "HostelOS API"
GET /swagger-ui.html   redirects/serves Swagger UI
GET /api/not-implemented 401 JSON
```

Production profile (`--spring.profiles.active=prod` on port 8082):

```text
GET /actuator/health   200
GET /v3/api-docs       404 JSON
GET /swagger-ui.html   404
GET /api/not-implemented 401 JSON
OPTIONS /api/not-implemented with Origin 401, no Access-Control-Allow-Origin
```

Flyway during runtime startup:

```text
Database: jdbc:mysql://localhost:3306/hostelos?... (MySQL 8.0)
Successfully validated 1 migration
Current version of schema `hostelos`: 1
Schema `hostelos` is up to date. No migration necessary.
```

No schema reset, clean, repair, data insert, or destructive migration was
performed. The `V1__baseline.sql` migration contains only `SELECT 1;`.

## CORS findings

CORS is not configured in either profile.

Evidence:

```text
OPTIONS /api/not-implemented
Origin: http://localhost:5173
Access-Control-Request-Method: GET
-> HTTP 401
-> no Access-Control-Allow-Origin header

Browser fetch from http://localhost:5176 to http://localhost:8080/actuator/health
-> TypeError: Failed to fetch
-> console: blocked by CORS policy: No 'Access-Control-Allow-Origin' header
```

The default frontend flow is unaffected because Vite proxies `/api` and the
browser sees a same-origin request. This is the safer local configuration.

Risk: any deployment or local setup that uses an absolute
`VITE_API_BASE_URL=http://localhost:8080/api` will fail in the browser. Do not
fix this with a wildcard origin. Add an explicit, profile-scoped allowlist when
direct-origin calls are actually needed, and keep production deny-by-default.

## Secret scan

- `Backend/.env` exists locally and contains `HOSTELOS_DB_USERNAME` and
  `HOSTELOS_DB_PASSWORD`. The username was verified to be `root`.
- `Backend/.gitignore` ignores `.env`, `.env.*`, and `.idea`, while keeping
  `.env.example`.
- `frontend/.gitignore` ignores `.env`, `.env.local`, and `.env.*.local`.
- `.env.example` files contain placeholders only:
  `HOSTELOS_DB_PASSWORD=change-me` and no real credentials.
- A value scan across the workspace (excluding local `.env`, `node_modules`,
  `target`, `dist`, and `.idea`) found no database password outside the local
  `.env`. The username string `root` appears in ordinary prose/code, which is
  not a secret.
- Because there is no Git repository, the report cannot prove what would be
  committed. This remains blocked until Git is initialized and `git status`
  confirms the local environment files are untracked.

Operational action: create a least-privilege MySQL user for HostelOS, replace
`root` in the local `.env`, and rotate the password if it was ever shared.

## Duplicate configuration and conflicts

1. **Version drift:** frontend `package.json`, `.env.example`, UI labels, and
   module docs use `0.1.1`; the backend Maven artifact is
   `0.0.1-SNAPSHOT`. No functional conflict, but release/version reporting will
   be inconsistent.
2. **Intentional defaults:** the MySQL URL appears both as a default in
   `application.properties` and in `Backend/.env.example`. The frontend API base
   appears in `.env.example`, `src/lib/env.js`, and UI text. These are
   documented fallbacks, not conflicting sources of truth, but they can drift if
   changed in only one place.
3. **Environment prefixes are correctly separated:** `VITE_` for public browser
   values, `HOSTELOS_` for backend/secret values. No collision was found.
4. **No missing dependencies:** frontend imports resolve and the build passes;
   backend dependency tree resolves and the application starts.
5. **Working-directory coupling:** the backend imports `.env` relative to the
   process working directory. Starting the jar from the repository root silently
   skips the optional file and then fails when credentials are absent. Start
   from `Backend/` or export the variables.
6. **No root build orchestration:** backend and frontend are independent; there
   is no root script, workspace manifest, or CI pipeline.

## Blocked checks

| Check | Reason | Required action |
|---|---|---|
| Git diff review | No Git repository | Initialize Git and approve baseline |
| Change preservation proof | No baseline or history | Establish baseline commit before further work |
| Tracked-secret proof | No Git index | Run `git status`/secret scan after initialization |
| CI gates | No workflows in `.github` | Add lint/test/build workflow |
| Full API workflow | No business endpoint or frontend endpoint | Implement Module 0.1.2 |
| Automated MySQL test | Tests use H2 | Add MySQL integration test or Testcontainers |
| Direct-origin CORS | No CORS configuration | Add explicit profile-scoped policy only if needed |
| Production deployment | Out of scope and not approved | Separate deployment review |

## Known defects

1. No Git repository.
2. No API surface and no connected frontend workflow.
3. No CORS configuration; direct cross-origin calls fail.
4. Backend artifact version does not match product version.
5. Local MySQL access uses `root`.
6. Flyway/H2 warning for H2 2.4.240.
7. Backend startup `.env` path is working-directory dependent.
8. Frontend README is the default Vite template.
9. No CI workflows.
10. Unrelated processes occupied local ports 4173, 4174, and 4175 during QA
    (4173 was identified as a `serve` process). QA used dedicated ports and
    stopped its own servers; this is environment interference, not an
    application defect.

## What was not changed

- No frontend source or configuration was edited.
- No backend source or configuration was edited.
- No database schema or data was modified beyond Flyway's read-only validation.
- No production deployment was performed.
- No commit was created because no repository exists and no approval was given.
- Build commands regenerated `frontend/dist` and `Backend/target`, which are
  build outputs, not source changes.
- Documentation was added under `docs/` and this report was created.

## Next module

**Module 0.1.2 - API Contract and Dev Integration Slice.**

The backend foundation agent proposed identity next. Integration evidence shows
that the missing piece is a real vertical slice: the frontend and backend have
never exchanged business data. Proving one endpoint end-to-end is a prerequisite
for identity and domain work.

Minimum acceptance criteria:

1. Add one read-only `/api/v1` endpoint with a reviewed response contract.
2. Add the matching RTK Query endpoint and render it with loading/success/error
   states.
3. Exercise the complete browser -> Vite proxy -> backend path with real data.
4. Add contract and authorization tests on both sides.
5. Decide and document the CORS policy; keep the proxy as the default unless a
   direct-origin need is approved.
6. Add CI gates, or document the remaining CI blocker.
7. Initialize Git and establish the baseline after approval.
8. Keep `npm run lint`, `npm test`, `npm run build`, and
   `mvnw -B clean verify` green.

Identity and authorization should follow as Module 0.1.3.
