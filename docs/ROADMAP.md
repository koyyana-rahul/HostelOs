# HostelOS Roadmap

Last updated: 2026-10-28
Basis: finalized Module 0.1.2 QA, backend stabilization evidence, and the
Module 0.1.3A/B plus 0.1.3A-R/0.1.3B-R backend-plus-frontend identity foundation

> Repository correction: earlier roadmap revisions reported that no Git
> repository existed. Git does exist at the HostelOS root, currently on branch
> `feature/0.1.3-identity-authorization` with commits `577a2b2` and `8f7b72b`.
> Module 0.1.3A/B, 0.1.3A-R, and 0.1.3B-R changes are uncommitted. There is still
> no root `.gitignore`, no CI, and no complete committed-secret history audit.

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

- The earlier "no Git repository" claim was incorrect; the repository and
  commits exist, as recorded in the correction note above.
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
residual risk. Tracked-secret verification, CI, and the production same-origin
proxy/CORS topology remain outstanding; Git itself exists.

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
8. Initialize Git and establish the baseline commit after approval. (Completed
   subsequently; the repository exists and has a baseline commit.)

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
- Technical release criteria are satisfied. Tracked-secret verification and CI
  remain unresolved repository-governance items; Git exists.
- No identity work was started during Module 0.1.2; Module 0.1.3A later added
  the backend identity foundation and Module 0.1.3B added the frontend workflow.

See `docs/INTEGRATION_QA_0.1.2.md` and
`Backend/docs/HOSTELOS_MODULE_0.1.2A_SECURITY_TEST_STABILIZATION.md`.

## Module 0.1.3 - Identity and Authorization Foundation (completed)

Module 0.1.3A delivered the backend identity and authentication foundation:

Module 0.1.3A delivered the backend identity and authentication foundation:

- Server-side Spring Security sessions with an HttpOnly `HOSTELOS_SESSION`
  cookie; no JWT.
- CSRF bootstrap and enforcement using `XSRF-TOKEN` and `X-XSRF-TOKEN`.
- `GET /api/v1/auth/csrf`, `POST /api/v1/auth/login`,
  `POST /api/v1/auth/logout`, and `GET /api/v1/auth/me`.
- A non-destructive Flyway `V2__identity_foundation.sql` for users, roles, and
  assignments.
- BCrypt cost 12; normalized login identifiers; `ACTIVE`/`DISABLED` status;
  server-side role assignments.
- A bounded in-memory login throttle: five failures per fifteen minutes.
- An optional environment-driven one-time administrator bootstrap; no seeded
  account.
- OpenAPI `sessionAuth` documentation and production Secure cookies.
- Backend clean verify: 40/40 tests; fresh MySQL 8 migration, validation,
  bootstrap, and auth flow verified.

Module 0.1.3A-R reconciled the role model:

- `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF` are distinct roles with no
  implicit hierarchy.
- Flyway `V3` renames the original `ADMIN` role to `PLATFORM_ADMIN` in place,
  preserving role IDs and `user_roles` assignments, and seeds
  `ORGANIZATION_ADMIN`.
- Existing `ADMIN` accounts retain their original platform-wide meaning; the
  bootstrap creates only `PLATFORM_ADMIN` when explicitly enabled.
- Organization administration is a role definition only; membership, tenant
  boundaries, and organization APIs remain deferred.
- Backend clean verify: 44/44 tests, including a V2-to-V3 upgrade compatibility
  test and explicit platform/organization/staff authorization boundaries.

Decision record: `docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md`.

Evidence: `Backend/docs/HOSTELOS_MODULE_0.1.3A_REPORT.md` and
`Backend/docs/HOSTELOS_MODULE_0.1.3A-R_ROLE_RECONCILIATION_REPORT.md`.

Module 0.1.3B delivered the frontend integration:

- Public `/login` with CSRF-protected credential submission and safe errors.
- `AuthGate` `/me` bootstrap and `RequireAuth` protected routes with intended
  destination preservation.
- Cookie-backed API requests, global `401` session clearing, and focus-based
  revalidation.
- Sidebar identity with informational server-provided role labels and
  CSRF-protected logout with RTK Query/Redux reset. Roles do not control
  navigation or actions.
- Revision 0.1.3B-R aligned fixtures, display, tests, and docs with the final
  `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF` names without adding
  permissions or admin screens.
- No passwords or tokens in browser storage.
- Frontend lint clean, 37/37 tests, production build successful.
- Real browser login, session restore, `/system` reload, logout, and post-logout
  `401` verified against a fresh MySQL schema.

Evidence: `docs/MODULE_0.1.3B_FRONTEND_REPORT.md`.

Still open in future modules:

- Add an administrative user-management API; bootstrap is currently the only
  account-creation path.
- Add organization membership and tenant boundaries before enforcing any
  organization-scoped authorization. `ORGANIZATION_ADMIN` currently grants no
  organization API access.
- Add MFA, password reset/account recovery, and audit logging.
- Add a shared rate limiter or edge protection for multi-instance deployment.
- Add a repeatable MySQL integration suite and a least-privilege application
  database user.
- Add CI and root ignore/secret governance.

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

1. Add a root `.gitignore` and approve the current uncommitted Module
   0.1.3A/B, 0.1.3A-R, and 0.1.3B-R change set before committing.
2. Complete a full tracked-secret history audit; the current filename audit
   found only templates/tests, but no CI or history scan exists.
3. Add CI for frontend lint/test/build and backend `clean verify`; preserve
   failing Surefire logs and enforce the production-chain guard.
4. Create a dedicated least-privilege MySQL user; rotate the current local
   credentials if they were shared.
5. Implement the production same-origin reverse-proxy topology; if separate
   origins are required, add a strict allowlist and preflight tests.
6. Align the backend Maven version with the product/module version.
7. Add a repeatable MySQL integration suite and checked-in browser auth
   automation.

Resolved during Module 0.1.2 finalization: the backend clean-verify flake was
stabilized, the `AVAILABLE` contract was accepted, and the development proxy
path was verified.

Resolved during Module 0.1.3A: Git was confirmed to exist, the backend identity
foundation and `V2` migration were implemented, the auth API was secured and
tested, and a fresh MySQL runtime pass completed. Git governance was partial
because the 0.1.3A/B changes were uncommitted and there was no root ignore/CI
policy. These items were resolved during Module 0.1.3C closure.

Resolved during Module 0.1.3B: the frontend now authenticates against the
session/CSRF API, restores sessions, guards workspace routes, renders identity,
and logs out; frontend gates and a real browser-to-backend flow passed.
Revision 0.1.3B-R aligned frontend fixtures, display, tests, and documentation
with the final `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF` names without
adding client-side permissions, role hierarchy, or admin screens. A
final-contract live run against the V3 jar verified login, the
`PLATFORM_ADMIN` label, session restore, logout, and post-logout `401`.

Resolved during Module 0.1.3A-R: platform administration is now distinct from
organization administration and staff operations; the legacy `ADMIN` role is
migrated in place without losing assignments; the bootstrap assigns only
`PLATFORM_ADMIN`; and authorization boundaries are covered by tests. The 0.1.3B-R
frontend revision uses the same three finalized names in its fixtures, display,
and regression tests.

## Module 0.1.3C - Final closure (2026-10-29)

Module 0.1.3C closed the identity and authorization foundation with a
`PASS WITH GAPS` verdict. CI workflows were implemented:

- **Backend CI**: `mvnw -B clean verify` on Java 17, 44/44 tests passed,
  Flyway V1/V2/V3 verified on H2 and disposable MySQL 8 schemas.
- **Frontend CI**: `npm ci`, `npm run lint`, `npm test -- --run`,
  `npm run build` all pass (37/37 tests, lint clean, build successful).
- **Repository governance**: root `.gitignore` added, excluding `Backend/.env`,
  `frontend/.env*`, `target/`, `dist/`, `coverage/`, and temp files.

Remaining gaps from Module 0.1.3 persist into future modules:
- No repeatable MySQL integration suite (H2 tests only; MySQL verification
  runtime one-off on disposable schemas)
- Process-local login throttling (5 failures/15 min, bounded to 10,000 keys)
- No distributed sessions or concurrent-session registry
- Organization administration deferred (`ORGANIZATION_ADMIN` is a role
  definition only; no membership model or tenant APIs)

### Later modules

These are directional and require their own approved scope:

| Module | Focus |
|---|---|
| 0.1.4 | Hostel, building, floor, room, and bed inventory |
| 0.1.5 | Residents, admissions, allocations, transfers, and vacating |
| 0.1.6 | Billing, invoices, payments, dues, and receipts |
| 0.1.7 | Operations: maintenance, complaints, visitors, and staff tasks |
| 0.1.8 | Dashboard reporting, exports, and audit views |
| 0.2.0 | Hardening: observability, backups, deployment, and performance |
