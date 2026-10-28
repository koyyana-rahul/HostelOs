# HostelOS Current State

Last verified: 2026-10-28 (Module 0.1.2 finalization)
Verified by: final technical release review of recorded stabilization and
frontend evidence, plus one runtime contract GET; the full gate matrices were
not rerun

## Summary

HostelOS has **one integrated read-only API workflow**. The `/system` page calls
`GET /api/v1/system/info` through the shared RTK Query client, the browser sends
a relative `/api` request, Vite proxies it to Spring Boot, and the page renders
the accepted metadata contract:

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

The real browser flow was verified with loading, success, safe error, retry, and
backend-outage recovery. `AVAILABLE` describes this metadata endpoint; it is not
a claim that MySQL or every dependency is healthy. `/actuator/health` remains the
dependency-health endpoint.

## Release status

| Gate | Status | Detail |
|---|---|---|
| Technical release criteria | **PASS** | Contract, security, backend stabilization, frontend gates, browser flow, and development proxy verified |
| Repository governance | **UNRESOLVED** | No Git repository, baseline, diff, tracked-file proof, or committed-secret verification |
| Production deployment | **DEFERRED** | Production same-origin proxy/CORS topology is documented but outside this development-slice release |
| Module 0.1.3 | **NOT STARTED** | Technically unblocked by Module 0.1.2; no identity work was started |

Module 0.1.2 is technically ready for release of the development integration
slice. Production deployment is deferred. The project owner still needs to
resolve Git governance before repository-release sign-off.

## Verified status

| Area | Status | Evidence |
|---|---|---|
| Frontend lint | PASS | `npm run lint`, exit 0, no ESLint errors |
| Frontend tests | PASS | 4 files, 13/13 tests |
| Frontend production build | PASS | 2002 modules, successful build |
| Frontend rendering | PASS | Browser: loading, success, safe 503 error/retry, real 502 outage/retry, responsive routes |
| Backend clean builds | PASS | Four `clean verify` runs, 19/19 tests each, all `BUILD SUCCESS` |
| Backend security repetition | PASS | 20 runs, 17/17 tests each, zero failures |
| Isolated failing paths | PASS | Five runs, 2/2 tests each, zero failures |
| Backend runtime / MySQL | PASS | Manual JDK 17 jar check; MySQL 8.0.40; Flyway schema version 1 up to date; automated tests use H2 |
| System API contract | PASS | `GET /api/v1/system/info` -> exact `HostelOS`, `v1`, `AVAILABLE` body; current runtime GET returned `200` |
| API security | PASS | Only the reviewed GET route is application-public; other methods and unlisted routes return `401` |
| Development proxy | PASS | Browser used relative `/api`; Vite forwarded to the backend; no direct browser-to-backend request |
| CORS | INTENTIONALLY UNCONFIGURED | Same-origin Vite proxy is the verified development topology; no CORS headers are granted |
| Health endpoint | PASS | `200` with liveness/readiness and no component details |
| OpenAPI / Swagger | PASS | Available in development; disabled under `prod` |
| Production profile | PASS | API docs/Swagger return `404`; health and system-info remain public |
| Git diff / change preservation | BLOCKED | No Git repository at the root, Backend, or frontend |
| Committed-secret verification | BLOCKED | No Git index or history; nested ignores exist but tracked status cannot be proven |
| CI gates | NOT IMPLEMENTED | No workflow runs lint, tests, build, or secret checks |

## Backend stabilization

The original QA failure is preserved in `docs/INTEGRATION_QA_0.1.2.md`: the first
clean verify failed 2/9 tests with `405`/`404` responses instead of `401`. The
exact historical trigger is unknown because the first failing Surefire log was
not retained.

The stabilization investigation reproduced the symptom when the production
`SecurityFilterChain` was absent while only the test `/probe/**` chain existed.
The production test class no longer imports the probe configuration and now
asserts that exactly one production chain exists. Production source code and
security policy were not changed.

Final evidence:

- Four clean `mvnw.cmd -B clean verify` runs: 19/19 tests passed, 0 failures,
  0 errors, 0 skipped.
- 20 repeated security runs: 17/17 tests passed in each run.
- Five isolated runs of the previously failing paths: 2/2 tests passed in each.
- Intentional negative-test stack traces remain test evidence, not application
  failures.

The residual risk is the unproven historical trigger. CI must preserve failing
Surefire logs and treat a missing production-chain guard as a release blocker.
See `Backend/docs/HOSTELOS_MODULE_0.1.2A_SECURITY_TEST_STABILIZATION.md`.

## Environment used

| Component | Value |
|---|---|
| OS | Windows, Git Bash |
| Node.js | 22.14.0 |
| npm | 10.8.1 |
| Frontend | Vite 8.3.5, React 19.3.1 |
| JDK | Microsoft OpenJDK 17.0.16 |
| Maven | Wrapper 3.9.9 |
| Spring Boot | 4.1.2 |
| MySQL | 8.0.40, service `MySQL80`, port 3306 |
| QA backend ports | 8081 development profile; 8082 production profile |
| Frontend recheck | Port 5175 with `VITE_API_PROXY_TARGET=http://localhost:8080` |
| Default ports | 8080/5173 were occupied by pre-existing IntelliJ/Vite processes; they were left untouched |

The shell's global `JAVA_HOME` is invalid and `java` is not on `PATH`. Backend
commands must set `JAVA_HOME` to the JDK 17 directory explicitly. The frontend
remains configured for the relative `/api` base URL by default.

## Outstanding issues

1. **No Git repository.** No baseline, diff, change attribution, or commit
   exists. This is the primary unresolved repository-governance decision.
2. **Committed-secret verification blocked.** No Git index or history exists.
   `Backend/.env` is covered by a nested ignore rule but cannot be proven
   untracked.
3. **No root `.gitignore`.** Nested ignores cover the current subprojects, but
   future root-level artifacts and secrets are unprotected by default.
4. **No CI.** The verified gates and stabilization repetitions are manual, and
   the historical flake has no automated run history.
5. **Production CORS/topology.** Development intentionally relies on the Vite
   proxy. A same-origin production reverse proxy remains deployment work;
   direct cross-origin access is unsupported by design.
6. **No authentication, authorization, business API, or multi-tenancy.** This
   module delivered one read-only metadata slice only.
7. **No automated cross-stack browser test.** Browser evidence came from a
   temporary QA script, not a repository-held CI test.
8. **Local MySQL uses `root`.** A least-privilege application user remains a
   follow-up.
9. **Backend artifact version drift.** `pom.xml` uses `0.0.1-SNAPSHOT` while
   product/UI documentation uses `0.1.1`.
10. **H2/Flyway support warning.** Flyway 12.4.0 reports H2 2.4.240 newer than
    its verified 2.3.232. The tests pass.
11. **Startup directory dependency.** `spring.config.import=optional:file:.env`
    resolves relative to the process working directory. Start the backend from
    `Backend/` or inject environment variables externally.
12. **Environment port interference.** Pre-existing 8080/5173 processes were
    active during verification; the QA/recheck processes used isolated frontend
    ports and were stopped afterward.
13. **MySQL integration testing is manual.** Automated backend tests use H2 in
    MySQL compatibility mode; no MySQL/Testcontainers integration suite exists.

## References

- `docs/INTEGRATION_QA_0.1.2.md` - full cross-stack QA and final technical decision.
- `Backend/docs/HOSTELOS_MODULE_0.1.2A_SECURITY_TEST_STABILIZATION.md` - flake
  investigation, fix, and final repeat evidence.
- `Backend/docs/HOSTELOS_MODULE_0.1.2A_REPORT.md` - original backend module evidence.
- `docs/MODULE_0.1.2B_FRONTEND_REPORT.md` - original frontend integration report.
- `docs/IMPLEMENTATION_LOG.md` - shared module history.
- `docs/ARCHITECTURE.md` - current design and boundaries.
