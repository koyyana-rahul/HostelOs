# HostelOS Roadmap

Last updated: 2026-10-28
Basis: finalized Module 0.1.2 QA, backend stabilization evidence, and frontend
release recheck

## Guiding principles

- Ship vertical slices: each module connects a real frontend workflow to a real
  backend API and persists data through Flyway-managed MySQL.
- Keep the modular monolith until a module has a demonstrated reason to split.
- Treat security, error contracts, tests, and documentation as part of the
  module, not follow-up work.
- Do not claim integration until a browser or API test exercises the full path.

## Module 0.1.1 - Foundation (complete, with caveats)

Delivered and verified:

- React/Vite shell, routing, Redux store, RTK Query base client, styling, and
  feedback states.
- Spring Boot baseline with environment-based MySQL configuration, Flyway
  baseline, deny-by-default security, JSON error contract, Actuator health, and
  OpenAPI.
- Frontend lint, tests, and build pass. Backend clean verify passes 7/7 tests.
- Health endpoint verified against MySQL in development and production
  profiles.

Caveats:

- No Git repository, so no diff, history, or commit exists.
- At 0.1.1 completion there was no business API and the frontend API client had
  no endpoints. Module 0.1.2 later added the read-only system-info endpoint.
- No CORS configuration; only the Vite proxy path works.
- Backend artifact version is `0.0.1-SNAPSHOT` while product documentation says
  `0.1.1`.
- Local database uses the MySQL `root` account.
- No CI workflows.

## Module 0.1.2 - API Contract and Dev Integration Slice (technically complete and release-ready; governance pending)

Purpose: prove one complete, non-speculative API workflow before adding
identity and domain modules. This is a thin vertical slice, not a feature
release.

Status on 2026-10-28: the backend exposes `GET /api/v1/system/info`; the
frontend `/system` page consumes it through the shared RTK Query client and the
Vite proxy. A real browser run requested the relative path, the proxy reached a
JDK 17 backend, and the page rendered `HostelOS`, `v1`, and `AVAILABLE`.
Loading, controlled error, retry, and real backend-outage recovery were
exercised. Frontend gates passed 13/13 tests and the production build.

The accepted contract is `HostelOS`, `v1`, and `AVAILABLE`. The earlier
integration brief's `UP` value is retained only as historical context.

The initial QA run's intermittent `405`/`404` security-test failure was
investigated. Stabilization passed four clean builds with `19/19` tests, 20
repeated security runs with `17/17` tests, and five isolated runs with `2/2`
tests. The exact historical trigger remains unknown and is documented as a
residual risk. Git, tracked-secret verification, CI, and the production
same-origin proxy/CORS topology remain outstanding.

Scope:

1. Add one reviewed, read-only backend endpoint under `/api/v1` (for example,
   `GET /api/v1/system/info` returning application name, version, and status).
2. Keep health public; require the deny-all rule for every other route.
3. Add an RTK Query endpoint for the new API and render its data in one frontend
   surface with loading, error, and empty states.
4. Document and test the response and error contracts.
5. Decide the local CORS policy. Prefer keeping the Vite proxy as the default;
   if direct-origin development is required, add an explicit dev-profile
   allowlist and preflight handling.
6. Add a browser-level integration test or scripted check that exercises the
   full frontend -> proxy -> backend path with real data.
7. Add lint, test, and build automation in CI, or explicitly document why CI
   remains blocked.
8. Initialize Git and establish the baseline commit after approval.

Acceptance criteria:

- One endpoint is reachable through the documented local path and returns real
  data.
- The frontend renders that data and handles failure without leaking internals.
- Backend tests cover success, validation/error, and authorization behavior.
- Frontend tests cover loading, success, and error states.
- CORS behavior is documented and tested if direct calls are enabled.
- No secrets are committed; `.env.example` remains the only committed template.
- `npm run lint`, `npm test`, `npm run build`, and `mvnw -B clean verify` pass.
- The integration report contains actual command output and any blocked checks.

Gate note: the backend clean-build criterion is satisfied by the stabilization
evidence: four consecutive `19/19` clean builds, 20 repeated security runs, and
five isolated failing-path runs passed. The original trigger is still not
reconstructable; CI log retention remains required.

## Module 0.1.2C - Cross-Stack QA and Release Gate (complete; technical PASS)

- Independent QA verified the real browser -> relative `/api/v1/system/info` ->
  Vite proxy -> Spring Boot -> RTK Query -> UI flow.
- Security verification confirmed only the reviewed GET route is
  application-public; unlisted routes and non-GET methods return JSON `401`.
- Health remains public; development OpenAPI/Swagger remain available; the
  `prod` profile disables them.
- The development proxy is the verified topology; CORS remains intentionally
  unconfigured.
- Backend stabilization resolved the technical test-gate hold with four clean
  `19/19` builds, 20 repeated `17/17` security runs, and five isolated `2/2`
  runs. The historical trigger remains unknown.
- Technical release criteria are satisfied. Git/tracked-secret verification and
  CI are unresolved repository-governance items.
- No Module 0.1.3 or identity work was started.

See `docs/INTEGRATION_QA_0.1.2.md` and
`Backend/docs/HOSTELOS_MODULE_0.1.2A_SECURITY_TEST_STABILIZATION.md`.

## Module 0.1.3 - Identity and Authorization Foundation (next, not started)

This was proposed by the backend foundation agent and is retained after the
integration slice. Module 0.1.3 is technically unblocked, but no Module 0.1.3
work has started. The project owner should resolve repository governance before
starting shared development:

- initialize Git and complete the baseline, diff, and tracked-secret audit;
- add CI that preserves failing logs and enforces the production-chain guard;
- decide whether the production same-origin proxy/topology is required before
  identity work or deferred to deployment.

Implementation scope for later:

- Approve the authentication model (session or token) before implementation.
- Add a non-destructive Flyway `V2` migration for users, roles, and authorities.
- Use a supported password encoder; no plaintext or seeded default credentials.
- Provide an approved bootstrap/admin mechanism using environment variables or
  a one-time command.
- Permit only explicitly reviewed authentication routes.
- Add OpenAPI security documentation.
- Test successful login, rejected login, disabled/locked users, unauthorized
  access, and secret non-disclosure.
- Validate the migration on H2 and on MySQL after database approval.
- Create a least-privilege MySQL application user and remove `root` from local
  configuration.

## Later modules

These are directional and require their own approved scope:

| Module | Focus |
|---|---|
| 0.1.4 | Hostel, building, floor, room, and bed inventory |
| 0.1.5 | Residents, admissions, allocations, transfers, and vacating |
| 0.1.6 | Billing, invoices, payments, dues, and receipts |
| 0.1.7 | Operations: maintenance, complaints, visitors, and staff tasks |
| 0.1.8 | Dashboard reporting, exports, and audit views |
| 0.2.0 | Hardening: observability, backups, deployment, and performance |

## Cross-cutting gates for every module

- Backend: `mvnw -B clean verify` passes; new Flyway migration reviewed.
- Frontend: lint, tests, and production build pass.
- Contract: frontend and backend agree on path, payload, and error shape.
- Security: deny-by-default preserved; only reviewed routes permitted; no
  wildcard CORS; no secrets in source.
- Data: no destructive migration without explicit approval and backup.
- Documentation: current state, architecture, and module report updated with
  verified output and blocked checks.
- Integration: at least one end-to-end path is exercised with real data before a
  module is called complete.

## Open prerequisites

1. Initialize Git and approve a baseline commit; add a root `.gitignore`.
2. Complete tracked-secret verification once Git history/index exists.
3. Add CI for frontend lint/test/build and backend `clean verify`; preserve
   failing Surefire logs and enforce the production-chain guard.
4. Create a dedicated least-privilege MySQL user; rotate the current local
   credentials if they were shared.
5. Implement the production same-origin reverse-proxy topology; if separate
   origins are required, add a strict allowlist and preflight tests.
6. Align the backend Maven version with the product/module version.

Resolved during Module 0.1.2 finalization: the backend clean-verify flake was
stabilized, the `AVAILABLE` contract was accepted, and the development proxy
path was verified.
