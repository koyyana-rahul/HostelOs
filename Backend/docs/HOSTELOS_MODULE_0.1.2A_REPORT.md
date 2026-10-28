# HostelOS Module 0.1.2A Report

> Repository correction (2026-10-28): the "no Git repository" statements in this
> historical report were incorrect. Git exists at the HostelOS root. Module
> 0.1.3A later added backend authentication; the limitations below describe this
> module's original state.

> Finalization: the security-test stabilization expanded the suite to 19 tests;
> four clean builds at 19/19, 20 repeated security runs at 17/17, and five
> isolated failing-path runs at 2/2 passed. The historical trigger remains
> unknown. See `HOSTELOS_MODULE_0.1.2A_SECURITY_TEST_STABILIZATION.md`.

## System Information API

Date: 2026-10-28  
Status: Complete  
Backend: Spring Boot 4.1.2  
Runtime verified with: Java 17.0.16, Maven 3.9.9

## Outcome

Implemented a public, versioned, non-sensitive application API endpoint:

```text
GET /api/v1/system/info
```

The endpoint returns only application metadata. It does not query the database, expose environment values, or claim dependency health.

## Contract

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

- HTTP status: `200 OK`
- Content type: `application/json`
- `status` means the system-information endpoint is available.
- `/actuator/health` remains the dependency-health endpoint.

## Files changed

Created:

- `src/main/java/com/rahul/HostelOs/system/SystemInfoController.java`
- `src/main/java/com/rahul/HostelOs/system/SystemInfoResponse.java`
- `docs/API.md`
- `docs/IMPLEMENTATION_LOG.md`
- `docs/HOSTELOS_MODULE_0.1.2A_REPORT.md`

Modified:

- `src/main/java/com/rahul/HostelOs/security/SecurityConfig.java`
- `src/test/java/com/rahul/HostelOs/HostelOsApplicationTests.java`

No frontend source file was changed.

## Security

- Security remains deny-by-default.
- Among application routes, only `GET /api/v1/system/info` is permitted.
- Existing infrastructure endpoints such as health, OpenAPI, Swagger UI, and `/error` remain public.
- `POST /api/v1/system/info` returns `401`.
- Unlisted application routes return JSON `401`.
- No broad `/api/**` permit was added.
- No authentication provider, fake user, or production authentication behavior was introduced.

## CORS decision

Development uses the existing Vite `/api` proxy, making the browser request same-origin. No CORS configuration, wildcard origin, or `@CrossOrigin` exception was added.

A separately hosted frontend and backend will require an explicit production-origin decision and strict origin allowlisting.

Runtime verification confirmed the proxied response has no `Access-Control-Allow-Origin` header.

## Automated verification

Command:

```powershell
$env:JAVA_HOME = 'C:\Users\RAHUL\.jdks\ms-17.0.16'
$env:Path = "C:\Users\RAHUL\.jdks\ms-17.0.16\bin;$env:Path"
.\mvnw.cmd -B clean verify
```

Result:

```text
BUILD SUCCESS
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
Total time: 20.164 s
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar
Jar size: 66,717,270 bytes
```

New tests:

- `systemInfoEndpointIsPublicAndReturnsStableContract` — verifies HTTP 200, JSON content type, and exact response body.
- `systemInfoEndpointOnlyPermitsGet` — verifies `POST` returns `401`.

The existing seven foundation tests were retained. The stack trace in the test output is intentional and comes from the existing sanitized-error test.

## Runtime verification

The packaged jar was started on port `8081` with Java 17.

| Check | Result |
|---|---|
| `GET /actuator/health` | `200` |
| `GET /api/v1/system/info` | `200` |
| Response content type | `application/json` |
| Response body | `{"applicationName":"HostelOS","apiVersion":"v1","status":"AVAILABLE"}` |
| `POST /api/v1/system/info` | `401` |
| `GET /api/v1/system/other` | `401` |
| OpenAPI path present | Yes |
| OpenAPI summary | `Get system information` |
| OpenAPI schema present | Yes |
| Vite proxy on port `5174` | `200`, identical body |
| CORS header through proxy | Absent |

The verification backend and Vite processes were stopped afterward, and ports `8081` and `5174` were confirmed free.

Verification note: the temporary cleanup script emitted a non-blocking warning while reading an empty stderr file after all checks had completed. It did not affect the application, build, or verification results.

## Database and secret safety

- No Flyway migration was added.
- No schema change was made.
- No `flyway clean`, `baseline`, `repair`, or reset was performed.
- No MySQL user or permission was changed.
- No `.env` credential was modified or printed.
- Runtime Flyway validation reported the existing schema as up to date at version 1.
- No secret appears in response payloads, source, or build configuration.

## Shared-file changes

These files are shared project documentation rather than module-private implementation:

- `docs/API.md` — added the exact API contract, security behavior, CORS/proxy decision, and OpenAPI locations.
- `docs/IMPLEMENTATION_LOG.md` — added the module implementation and verification record.
- `docs/HOSTELOS_MODULE_0.1.2A_REPORT.md` — added this report.

The existing `docs/HOSTELOS_MODULE_0.1.1_REPORT.md` was not modified.

## Known limitations and follow-ups

- The local `.env` still uses the local MySQL `root` account. Create a dedicated least-privilege application user before sharing or deployment.
- IntelliJ may still run the project with Java 22. Set the project SDK to `ms-17` for parity with the verified build.
- No authentication or tenancy existed at the time of this module; Module 0.1.3A
  later added backend authentication.
- The historical "no Git repository" statement was incorrect; Git exists. No
  module commit was created at the time.
- `GET /api/v1/system/info` is intentionally the only application API endpoint in this module.
