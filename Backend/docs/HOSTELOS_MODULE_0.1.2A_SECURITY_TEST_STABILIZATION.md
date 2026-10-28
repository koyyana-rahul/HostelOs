# HostelOS Module 0.1.2A — Security Test Stabilization Report

Date: 2026-10-28
Status: Complete
Scope: Intermittent security integration-test failures in the Module 0.1.2 release gate

## Summary

The observed `405` and `404` responses were reproduced by a test context in which the production `SecurityFilterChain` was absent while a test-only `/probe/**` chain was present. The production authorization policy was not defective, and no production code was changed.

The tests were refactored to isolate the probe configuration and to fail explicitly if the production chain is missing. After the refactor, four clean verifications passed with 19/19 tests, 20 repeated security runs passed with 17/17 tests each, and five isolated runs of the previously failing paths passed with 2/2 tests each.

## Original Failure

The first clean verification reported:

```text
Tests run: 9, Failures: 2, Errors: 0, Skipped: 0
POST /api/v1/system/info  expected 401, received 405
GET /api/not-implemented expected 401, received 404
```

Subsequent runs passed without a source change. The first failing Surefire log was not retained.

## Investigation

The current production configuration was inspected and exercised:

- `SecurityConfig` permits only `GET /api/v1/system/info` among application routes.
- Health, OpenAPI, Swagger UI, and `/error` remain public infrastructure endpoints.
- All other application requests are denied.
- `ApiSecurityErrorHandler` writes JSON `401` responses with `Authentication is required`.
- With the production chain active, the previously failing requests return the expected `401`.

Pre-fix repetition results:

| Check | Result |
|---|---|
| Full test class, 5 runs | 5 passed |
| Random-order runs, 10 runs | 10 passed |
| Specific failing tests in isolation | Passed |
| QA first clean verify | 2/9 failed once |
| Controlled reproduction | 2 exact failures |

The original failure was not reproducible by test order alone, and the first failing log was unavailable.

## Root Cause

A controlled reproduction removed the production `securityFilterChain` bean and retained only a test chain scoped to `/probe/**`. That produced exactly the observed failures:

- Spring Boot's default security chain backs off when any `SecurityFilterChain` bean exists.
- The probe chain matched only `/probe/**`.
- `/api/**` reached MVC without production authorization.
- MVC returned `405` for the existing system-information path and `404` for the unlisted path.

Adding a second catch-all chain while the production chain remains is not a valid explanation: Spring Security 7.1.2 rejects multiple catch-all chains during application-context startup, producing a startup failure rather than `405`/`404`.

The reproduced mechanism therefore required `/api/**` to bypass the production chain; in the controlled case, the production chain bean was absent. The exact reason it was not applied during the first QA run is not reconstructable from the available evidence. This is a test-context isolation defect, not a production authorization regression.

## Fix

Production code and policy were not modified.

### Production security tests

`HostelOsApplicationTests` remains the only class that asserts production `/api` authorization. It no longer imports the probe configuration.

The class now:

- Autowires `List<SecurityFilterChain>`.
- Asserts exactly one chain exists.
- Asserts that chain is the production `SecurityConfig` chain.
- Verifies `GET /api/v1/system/info` returns the exact contract.
- Verifies POST, HEAD, PUT, DELETE, and PATCH are denied with `401`.
- Verifies HEAD has no response body.
- Verifies an unlisted route is denied with `401` JSON.
- Retains health, OpenAPI, Swagger UI, and context-startup checks.

### Error-handling tests

`ErrorHandlingIntegrationTests` owns the test-only probe configuration. Its custom chain is scoped to `/probe/**` and is not used for any production security assertion.

It verifies:

- Validation failures use the shared `400` error shape.
- Unexpected failures return a sanitized `500` response without internal details.

### Production-profile tests

`ProductionDocumentationTests` activates the `test` and `prod` profiles and verifies:

- `/v3/api-docs` returns `404`.
- `/swagger-ui/index.html` returns `404`.
- `/api/v1/system/info` remains public.
- An unlisted route remains denied.
- `/actuator/health` remains public.

## Verification

### Clean builds

Command:

```powershell
$env:JAVA_HOME = 'C:\Users\RAHUL\.jdks\ms-17.0.16'
$env:Path = "C:\Users\RAHUL\.jdks\ms-17.0.16\bin;$env:Path"
.\mvnw.cmd -B clean verify
```

| Run | Tests | Failures | Errors | Skipped | Build | Time |
|---|---:|---:|---:|---:|---|---:|
| 1 | 19 | 0 | 0 | 0 | SUCCESS | 18.338 s |
| 2 | 19 | 0 | 0 | 0 | SUCCESS | 18.335 s |
| 3 | 19 | 0 | 0 | 0 | SUCCESS | 18.103 s |
| 4 | 19 | 0 | 0 | 0 | SUCCESS | 19.270 s |

### Repeated security tests

Command:

```powershell
.\mvnw.cmd -B "-Dtest=HostelOsApplicationTests,ProductionDocumentationTests" test
```

| Check | Result |
|---|---|
| Runs | 20 |
| Tests per run | 17 |
| Failed runs | 0 |
| Failed tests | 0 |

### Isolated failing paths

Command:

```powershell
.\mvnw.cmd -B "-Dtest=HostelOsApplicationTests#systemInfoEndpointRejectsPost+unlistedEndpointsAreDenied" test
```

| Check | Result |
|---|---|
| Runs | 5 |
| Tests per run | 2 |
| Failed runs | 0 |
| Failed tests | 0 |

### Toolchain and artifact

```text
Apache Maven 3.9.9
Java 17.0.16 (Microsoft OpenJDK)
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar
Jar size: 66,717,270 bytes
```

## Release-Gate Decision

The stabilization is sufficient for the Module 0.1.2 release gate:

- Production policy remains exact GET-only authorization for the system-information endpoint.
- The production test context no longer contains a competing test chain.
- A missing production chain now causes an explicit test failure instead of allowing probe-only coverage to pass.
- Four clean builds and 20 repeated security runs passed.

The residual risk is that the exact historical trigger for the first QA failure is unknown. CI should preserve failing Surefire logs and treat a missing production-chain guard failure as a release blocker.

## Constraints Observed

- No production source file was changed.
- No migration or schema change was made.
- No database reset, repair, clean, or baseline was performed.
- No MySQL credential or permission was changed.
- No `.env` content was printed or modified.
- No frontend code was changed.
- The historical "no Git repository" statement was incorrect; Git exists. No
  commit was created during stabilization.
- No Module 0.1.3 identity work was started during stabilization; Module 0.1.3A
  later added the backend identity foundation.

## Evidence Locations

- `C:\Users\RAHUL\AppData\Local\Temp\opencode\hostelos-flake\clean-verify-after-1.log`
- `C:\Users\RAHUL\AppData\Local\Temp\opencode\hostelos-flake\clean-verify-after-2.log`
- `C:\Users\RAHUL\AppData\Local\Temp\opencode\hostelos-flake\clean-verify-after-3.log`
- `C:\Users\RAHUL\AppData\Local\Temp\opencode\hostelos-flake\clean-verify-after-4.log`
- `C:\Users\RAHUL\AppData\Local\Temp\opencode\hostelos-flake\security-repeat-01.log` through `security-repeat-20.log`
- `C:\Users\RAHUL\AppData\Local\Temp\opencode\hostelos-flake\security-isolated-01.log` through `security-isolated-05.log`
- `C:\Users\RAHUL\AppData\Local\Temp\opencode\hostelos-flake\repro-missing-production-chain.log`
