# HostelOS Module 0.1.2 - Cross-Stack Integration QA

Date: 2026-10-28
Scope: Module 0.1.2 - API Contract and Dev Integration Slice
Reviewer role: independent cross-stack QA and release gate
Initial QA result: **PASS WITH GAPS**
Initial release-gate decision: **NOT APPROVED (HOLD)**
Finalized technical result: **TECHNICAL RELEASE CRITERIA SATISFIED**
Finalized governance status: **UNRESOLVED - Git and tracked-secret audit absent**

## Finalization update (2026-10-28)

The initial QA report below is retained as the historical record. Subsequent
backend stabilization and frontend recheck changed the final technical outcome:

- The intermittent security-test failure was investigated and reproduced as a
  test-context isolation defect, not a production authorization defect.
- Four clean `mvnw.cmd -B clean verify` runs passed `19/19` tests each; 20
  repeated security-test runs passed `17/17` each; five isolated runs of the
  previously failing paths passed `2/2` each.
- The production test suite now asserts that exactly one production
  `SecurityFilterChain` exists and that it is the `SecurityConfig` chain. The
  probe chain is isolated in `ErrorHandlingIntegrationTests`.
- The contract is accepted as `GET /api/v1/system/info` returning
  `HostelOS`, `v1`, and `AVAILABLE`. The earlier brief's `UP` value is retained
  only as historical context. A final non-destructive runtime GET returned
  `200` with that exact body.
- The frontend recheck passed lint, build, and `13/13` tests and independently
  exercised the real browser-to-proxy-to-backend flow.
- The finalization activity reviewed the recorded stabilization and frontend
  recheck evidence and executed one runtime contract request; it did not rerun
  the full backend or frontend gate matrices.
- The exact historical trigger of the original failure remains unknown. The
  stabilization report records that limitation and the required CI safeguard.

Technical criteria are satisfied. Git initialization, tracked-secret
verification, and CI remain unresolved repository-governance items.

## Executive result

The system-information slice works end to end. A real browser requested the
relative `/api/v1/system/info` path, Vite proxied it to a Spring Boot instance
running on JDK 17, the controller returned JSON with HTTP `200`, and RTK Query
rendered `HostelOS`, `v1`, and `AVAILABLE`. Loading, safe error, retry, and real
backend-outage recovery were exercised in the browser.

Security behavior is consistent with the design: only `GET
/api/v1/system/info` is application-public, all other tested application routes
and methods return JSON `401`, health remains public, development OpenAPI/Swagger
remain available, and the `prod` profile disables them. No CORS allowance was
added.

At initial QA, the release gate was held for three reasons:

1. The first required backend `clean verify` run failed 2/9 tests. The immediate
   rerun and five additional test runs passed 9/9, but the flake had no confirmed
   root cause. This was addressed by the stabilization work summarized above;
   the historical trigger remains unknown.
2. Git is not initialized, so tracked-file, diff, and committed-secret
   verification cannot be completed. This governance item remains unresolved.
3. The earlier integration brief expected `status: "UP"`; the backend contract,
   frontend fixture, and backend documentation use `"AVAILABLE"`. This was
   resolved by accepting `AVAILABLE` as the contract.

No application source code was changed during the original QA pass. Only
documentation was updated; the mandated build commands regenerated
`Backend/target` and `frontend/dist`. The later backend stabilization changed
test code only.

## Repository and change-safety audit

Commands:

```text
git rev-parse --show-toplevel       -> exit 128, fatal: not a git repository
git status --short                  -> exit 128, fatal: not a git repository
git -C Backend rev-parse ...        -> exit 128
git -C frontend rev-parse ...       -> exit 128
```

- There is no Git repository at `D:\Downloads\HostelOs`, `Backend`, or
  `frontend`.
- No baseline, diff, history, tracked-file list, or commit exists. Change
  attribution and tracked-secret verification are therefore **BLOCKED**.
- Git was not initialized and no commit was created.
- The root has no `.gitignore`. Nested ignore files do cover:
  - `frontend/`: `node_modules`, `dist`, coverage, `.env`, `.env.local`,
    `.env.*.local`, and common IDE/OS files.
  - `Backend/`: `target/`, `.env`, `.env.*` (with `.env.example` re-included),
    Maven IDE metadata, and IntelliJ/STS/VS Code files.
- `frontend/.env.test` is intentionally not ignored and contains only a local
  test origin; no secret value was observed in it.
- `Backend/.env` is present and covered by the nested ignore rule, but without
  Git it cannot be proven untracked.
- Existing build outputs were left in place:
  - `Backend/target/` (approximately 64 MB; jar rebuilt during QA).
  - `frontend/dist/` (approximately 465 KB; rebuilt during QA).
  - `frontend/node_modules/` and `Backend/.idea/` are present and ignored by
    nested rules.
- No secret values were printed during this review.

## Contract verification

| Check | Result | Evidence |
|---|---|---|
| Endpoint path | PASS | Frontend base `/api` + query `/v1/system/info`; backend `/api/v1/system` + `/info`; browser requested `/api/v1/system/info` |
| HTTP method | PASS | `GET`; browser request was `GET`; non-GET methods returned `401` |
| Response status | PASS | Direct and proxied requests returned `200` |
| Content type | PASS | `application/json` |
| Response fields | PASS | `applicationName`, `apiVersion`, `status`; OpenAPI schema shows all three as strings |
| Response values | PASS | Accepted contract: `HostelOS`, `v1`, `AVAILABLE`; the earlier brief expected `UP` |
| JSON body | PASS | Exact body: `{"applicationName":"HostelOS","apiVersion":"v1","status":"AVAILABLE"}` |
| Secret/internal exposure | PASS | Only the three metadata strings; raw error responses use the shared safe error shape |
| Error conventions | PASS | Security failures return `ApiError` JSON; validation and unexpected-error contracts are covered by the stabilized 19-test backend suite |
| Frontend validation | PASS | Missing/empty fields show a safe error; returned values are displayed unchanged |
| Frontend error safety | PASS | Controlled 503 and real 502 outage displayed generic messages, not upstream details |

The finalization decision accepts `"AVAILABLE"` as the contract. The backend,
frontend fixture/tests, and API documentation already agree. The earlier
brief's `"UP"` value is retained only as historical context; it is not a
remaining contract defect.

## Security verification

Static review of `SecurityConfig`:

- `GET /api/v1/system/info` is the only application API route permitted.
- Health, OpenAPI, Swagger UI, and `/error` remain public as before.
- `anyRequest().denyAll()` remains in place.
- No wildcard `/api/**` permit, CORS configuration, `@CrossOrigin`, or security
  bypass was found.
- CSRF remains disabled because the API is stateless and has no session flow.
- Test-only `/probe/**` permits exist only in the test configuration.

Runtime checks against the JDK 17 development instance on port `8081`:

| Request | Result |
|---|---|
| `GET /actuator/health` | `200`, `{"groups":["liveness","readiness"],"status":"UP"}`, no components/details |
| `GET /api/v1/system/info` | `200`, `application/json`, exact expected body |
| `POST /api/v1/system/info` | `401` JSON |
| `HEAD/PUT/DELETE/PATCH /api/v1/system/info` | `401` JSON |
| `GET /api/v1/system/other` | `401` JSON |
| `GET /api/not-implemented` | `401` JSON |
| `OPTIONS /api/v1/system/info` preflight | `401`, no `Access-Control-Allow-Origin` |
| `GET /api/v1/system/info` with `Origin` | `200`, no `Access-Control-Allow-Origin` |
| `GET /v3/api-docs` | `200`, path, GET operation, `SystemInfoResponse` schema present |
| `GET /swagger-ui/index.html` | `200` in development |
| `GET /v3/api-docs`, `/swagger-ui.html`, `/swagger-ui/index.html` with `prod` | `404` JSON |
| `prod` health and system-info GET | `200`; unlisted route `401` |

No broad CORS allowance or authentication bypass was introduced. Direct
cross-origin browser access remains unsupported by design.

## Real integration evidence

### Controlled environment

The default ports `8080` and `5173` were already occupied by a separately
launched IntelliJ backend (Java 22) and Vite session. Those processes were left
untouched. QA used isolated ports:

- Backend: packaged `HostelOs-0.0.1-SNAPSHOT.jar`, JDK 17.0.16, MySQL 8.0.40,
  port `8081`; Flyway reported schema version 1 as up to date.
- Frontend: Vite 8.3.5, port `5174`, explicit
  `VITE_API_PROXY_TARGET=http://localhost:8081`.
- Production-profile backend: same jar on port `8082`.

The proxy target was proven by stopping only the `8081` instance: the browser
received a Vite `502` while the unrelated `8080` backend stayed healthy. After
restarting `8081`, the page's retry succeeded with `200`.

### Browser flow

A headless browser loaded `http://localhost:5174/system`:

1. The rendered page issued `GET http://localhost:5174/api/v1/system/info`.
2. The request path was exactly `/api/v1/system/info`; the browser never
   contacted port `8081` directly.
3. Vite forwarded it to the controlled backend and received the expected JSON.
4. RTK Query rendered `HostelOS`, `v1`, and `AVAILABLE`.
5. Page title was `System · HostelOS`; heading was `System information`.
6. A delayed first response displayed `Contacting the backend` and the request
   path before completing.
7. A controlled `503` response showed `System information unavailable` without
   `internal database detail`, then Retry produced a real `200`.
8. A real backend outage produced a Vite `502`; restarting the backend and
   clicking Retry produced a real `200`.
9. The successful clean reload had no alert, no page errors, and no unexpected
   console errors. The only console entries were the deliberately induced
   `503` and `502` responses.
10. Desktop and responsive screenshots were inspected; no clipping or
    horizontal overflow was observed.

`GET http://localhost:5174/api/v1/system/info` also returned `200` with the
identical body from `curl`, confirming the proxy path separately from the
browser state.

## Build and regression results

### Backend

Required command:

```text
JAVA_HOME=C:/Users/RAHUL/.jdks/ms-17.0.16
mvnw.cmd -B clean verify
```

Toolchain: Apache Maven 3.9.9, Java 17.0.16 (Microsoft OpenJDK).

Run 1:

```text
Tests run: 9, Failures: 2, Errors: 0, Skipped: 0
BUILD FAILURE
Exit code: 1
```

Failures:

- `systemInfoEndpointOnlyPermitsGet`: expected `401`, received `405`.
- `unlistedEndpointsAreDenied`: expected `401`, received `404`.

Run 2:

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Exit code: 0
Total time: 19.767 s
```

Five additional `mvnw.cmd -B test` runs each returned exit code `0` with
`9/9` tests passing. The failure did not reproduce. The first failing run
overlapped with an IDE-launched backend being started from `target/classes` and
with the frontend gates; a causal link was not established. At that point the
flake was a release-gate gap, not a confirmed production failure.

### Stabilization follow-up

The subsequent stabilization report documents the root-cause investigation and
final evidence. The original 9-test suite was expanded to 19 tests. The probe
configuration was moved out of the production-security test class, and the
production test now fails explicitly if the production `SecurityFilterChain` is
missing.

| Verification | Result |
|---|---|
| Four clean `mvnw.cmd -B clean verify` runs | `19/19` tests passed in each; `BUILD SUCCESS`; 18.338 s, 18.335 s, 18.103 s, 19.270 s |
| 20 repeated security runs | `17/17` tests passed in each; zero failed runs |
| Five isolated runs of the previously failing paths | `2/2` tests passed in each; zero failed runs |
| Historical failure trigger | Exact trigger remains unknown; first failing Surefire log was not retained |

The controlled reproduction showed that removing the production security chain
while retaining only the `/probe/**` test chain produced the observed `405` and
`404` responses. This is a test-context isolation defect; no production
authorization code or policy was changed. The residual risk is the unproven
historical trigger, so CI must preserve failing Surefire logs and treat a
missing production-chain guard as a release blocker.

Evidence: `Backend/docs/HOSTELOS_MODULE_0.1.2A_SECURITY_TEST_STABILIZATION.md`.

Final jar: `Backend/target/HostelOs-0.0.1-SNAPSHOT.jar`, 66,717,270 bytes.

Unresolved warnings:

- Flyway 12.4.0 reports H2 2.4.240 newer than its verified 2.3.232.
- SpringDoc logs that its endpoints are enabled by default; the `prod` profile
  correctly disables them and runtime verification returned `404`.
- The intentional validation/error test logs a stack trace and a `POST`
  security rejection; this is test evidence, not an unexpected application
  failure.

### Frontend

Latest frontend release-recheck results:

| Command | Exit | Result |
|---|---:|---|
| `npm ls --depth=0` | 0 | Clean dependency tree |
| `npm run lint` | 0 | No ESLint errors |
| `npm test` | 0 | 4 files, 13/13 tests passed, 4.88 s |
| `npm run build` | 0 | 2002 modules, built in 1.06 s |

Main bundle was 404.08 kB (129.38 kB gzip); the `SystemPage` chunk was 7.85 kB
(2.47 kB gzip). No unresolved frontend build warnings were reported. The
release recheck repeated the real browser flow at desktop and mobile widths:
loading, success, safe `503` error, retry, route navigation, and responsive
navigation. The only console error was the intentional `503`; no page errors
occurred.

Mocked tests and the real browser run are separate evidence. The browser run,
not the mocked tests, is the end-to-end proof for this report.

## CORS decision

- **Development:** same-origin browser request to Vite, with `/api` proxied to
  the backend. No CORS headers are added.
- **Production:** prefer serving the frontend and proxying `/api` through the
  same origin (reverse proxy or equivalent). This avoids browser cross-origin
  requests for the initial release.
- **Future separate origins:** configure a strict, explicit origin allowlist,
  scope it to the required profiles, and test allowed-origin, rejected-origin,
  and preflight behavior. Do not use a wildcard with credentials.

No CORS change was made in this module; leaving it unconfigured is intentional
while development uses the same-origin proxy. Preflight currently returns `401`
and no `Access-Control-Allow-Origin`, which is the expected deny-by-default
behavior.

## Module boundary

This is one read-only integration slice. It is not a completed business API,
authentication system, authorization model, or multi-tenancy implementation.
No domain tables, authentication provider, user store, or protected business
workflow exists. Module 0.1.3 must not be considered started by this review.

## Documentation and evidence changes

No application source or configuration was modified during the original QA
pass. The later stabilization changed backend test code only.

Updated or created during the initial QA:

- `docs/INTEGRATION_QA_0.1.2.md` (this report)
- `docs/CURRENT_STATE.md`
- `docs/ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/IMPLEMENTATION_LOG.md` (new shared log)

Updated during finalization:

- `docs/INTEGRATION_QA_0.1.2.md` (final technical and governance decision)
- `docs/CURRENT_STATE.md`
- `docs/ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/IMPLEMENTATION_LOG.md`
- `docs/MODULE_0.1.2B_FRONTEND_REPORT.md`
- `frontend/README.md` (contract accepted as `AVAILABLE`)
- `Backend/docs/HOSTELOS_MODULE_0.1.2A_REPORT.md`

Regenerated by the required gates, not deleted:

- `Backend/target/`
- `frontend/dist/`

Implementation files inspected:

- `Backend/src/main/java/com/rahul/HostelOs/system/SystemInfoController.java`
- `Backend/src/main/java/com/rahul/HostelOs/system/SystemInfoResponse.java`
- `Backend/src/main/java/com/rahul/HostelOs/security/SecurityConfig.java`
- `Backend/src/main/java/com/rahul/HostelOs/security/ApiSecurityErrorHandler.java`
- `Backend/src/main/java/com/rahul/HostelOs/common/exception/ApiError.java`
- `Backend/src/main/java/com/rahul/HostelOs/common/exception/GlobalExceptionHandler.java`
- `Backend/src/test/java/com/rahul/HostelOs/HostelOsApplicationTests.java`
- `frontend/src/features/system/api/systemApi.js`
- `frontend/src/features/system/lib/systemInfo.js`
- `frontend/src/features/system/pages/SystemPage.jsx`
- `frontend/src/services/api/baseApi.js`
- `frontend/src/lib/env.js`
- `frontend/src/app/routes.js`
- `frontend/src/components/layout/navigation.js`
- `frontend/vite.config.js`
- `frontend/tests/features/systemApi.test.js`
- `frontend/tests/features/SystemPage.test.jsx`

## Remaining gaps after finalization

1. **Historical flake trigger unknown.** The controlled reproduction identified
   a test-context isolation mechanism, and the new guard plus repeated runs are
   strong evidence, but the exact trigger from the first QA run cannot be
   reconstructed. CI must retain failing Surefire logs and treat a missing
   production-chain guard as a release blocker.
2. **No Git repository.** Diff, change preservation, tracked-file status, and
   committed-secret verification are impossible. No commit exists. This is a
   repository-governance blocker, not an unresolved application defect.
3. **No root `.gitignore`.** Nested ignores cover the current subprojects, but
   future root-level artifacts and secrets are unprotected by default.
4. **No CI.** The frontend and backend gates are manual; the stabilization
   history lives in local logs rather than an automated pipeline.
5. **Production CORS/topology not implemented.** Development intentionally uses
   the same-origin Vite proxy with no CORS configuration. A same-origin
   production reverse proxy remains deployment design; direct cross-origin
   access is unsupported by design.
6. **No authentication, authorization, business API, or multi-tenancy.** Out of
   scope for this module and explicitly not delivered.
7. **No automated cross-stack browser test in the repository.** The successful
   browser evidence came from a temporary QA script; it is not a repeatable CI
   gate.
8. **Pre-existing environment risks remain:** local MySQL uses `root`, backend
   Maven version is `0.0.1-SNAPSHOT` while product/UI docs use `0.1.1`, backend
   startup depends on running from `Backend/`, and Flyway warns about H2
   version support.
9. **MySQL integration testing is manual.** Automated backend tests use H2 in
   MySQL compatibility mode; MySQL runtime behavior is verified manually, not
   by a repository-held automated integration suite.

## Release-gate decision

**TECHNICAL RELEASE CRITERIA SATISFIED. Repository governance remains
unresolved.**

Technical readiness:

- The accepted contract is `GET /api/v1/system/info` returning `HostelOS`,
  `v1`, and `AVAILABLE`; the backend, frontend, tests, and documentation agree.
- Security remains deny-by-default, with only the reviewed GET route public.
- The backend stabilization passed four clean builds (`19/19` tests), 20
  repeated security runs (`17/17` tests), and five isolated runs (`2/2` tests).
- The frontend passed lint, build, and `13/13` tests, and the real
  browser-to-backend flow was exercised through the Vite proxy.
- CORS remains intentionally unconfigured for the verified development
  topology. Production same-origin proxying is a deployment follow-up.

Repository governance:

- Git is still absent. There is no baseline, diff, tracked-file proof, or
  committed-secret verification.
- There is no root `.gitignore` and no CI pipeline.
- The project owner must initialize Git with an approved baseline and complete
  the secret/change review before repository release sign-off.

Residual technical risk: the exact historical trigger of the first flaky test
run is unknown. The stabilization guard and repeated-run evidence reduce that
risk, but CI log retention is required if it recurs.

Module 0.1.2 is technically ready for release of the development integration
slice, and the next development module is technically unblocked. Production
deployment remains deferred. Module 0.1.3 was not started.
