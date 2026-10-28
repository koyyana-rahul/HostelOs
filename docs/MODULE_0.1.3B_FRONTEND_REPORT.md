# Module 0.1.3B - Frontend Identity Integration Report

Date: 2026-10-28
Revision: Module 0.1.3B-R frontend role contract alignment with 0.1.3A-R
Scope: frontend integration of the session/CSRF/identity API and reconciled roles
Result: implemented, tested, and verified through a real browser and backend

## Outcome

The frontend now uses the backend's server-managed session model:

- A responsive `/login` page authenticates through
  `POST /api/v1/auth/login` after obtaining a CSRF token from
  `GET /api/v1/auth/csrf`.
- Session restore uses `GET /api/v1/auth/me`; unauthenticated visitors are sent
  to `/login` with their intended internal path preserved.
- Existing workspace routes remain available only inside the authenticated route
  boundary.
- The current identity, login identifier, and server-assigned roles are shown in
  the sidebar.
- Logout requests a fresh CSRF token, calls `POST /api/v1/auth/logout`, clears
  RTK Query and Redux auth state, and returns to `/login`.
- A `401` from any API request clears local session state. Background identity
  revalidation runs when the window regains focus.

No JWT or browser token storage was introduced. The HttpOnly `HOSTELOS_SESSION`
cookie remains the session credential, and the frontend sends cookies with every
RTK Query request.

## Revision 0.1.3B-R - Role contract alignment

The finalized backend contract was re-inspected after platform-role
reconciliation:

- `Backend/src/main/java/com/rahul/HostelOs/identity/RoleName.java` contains
  `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF`.
- `V3__reconcile_platform_admin_role.sql` renames `ADMIN` to `PLATFORM_ADMIN` in
  place, preserves role IDs and assignments, and seeds `ORGANIZATION_ADMIN`.
- `AuthUserResponse` returns those bare names in the `roles` array.
- `ROLE_PLATFORM_ADMIN`, `ROLE_ORGANIZATION_ADMIN`, and `ROLE_STAFF` are internal
  Spring Security authorities, not values returned by the identity API.
- There is no implicit role hierarchy. `ORGANIZATION_ADMIN` is a role definition
  only; organization membership and tenant-scoped enforcement remain deferred.

The frontend now aligns its fixtures, tests, documentation, and identity display
with that contract:

- Added explicit `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF` test fixtures
  plus a STAFF sign-in regression.
- Role labels are normalized only for string validity, trimming, and duplicate
  removal; their case and meaning are not changed.
- The sidebar displays a `Roles` section containing the exact server values, a
  server-assigned-roles accessible name, and an explicit note that roles are
  informational and access is enforced by the backend.
- Added regression coverage for the exact `/me` role response, multi-role
  rendering, STAFF authentication, and navigation that remains independent of
  role labels.
- No route, navigation, control, or privileged action is conditioned on a role.
  No platform- or organization-administration screen or fake permission was
  added.
- The cookie-backed session, CSRF handling, login/logout, safe errors, route
  guards, and existing routes are unchanged.

## Frontend changes

| File | Change |
|---|---|
| `frontend/src/features/auth/authSlice.js` | Explicit checking, authenticated, anonymous, and unavailable session states. |
| `frontend/src/features/auth/api/authApi.js` | `me`, CSRF, login, and logout endpoints injected into the shared RTK Query API. |
| `frontend/src/features/auth/components/AuthGate.jsx` | Bootstraps the server session and renders loading or retryable unavailable states. |
| `frontend/src/features/auth/components/RequireAuth.jsx` | Redirects anonymous users to `/login` and preserves the requested internal location. |
| `frontend/src/features/auth/pages/LoginPage.jsx` | Responsive, accessible sign-in form with safe error mapping, validation, CSRF bootstrap, and destination restore. |
| `frontend/src/features/auth/hooks/useLogout.js` | CSRF-protected logout, local state reset, and failure handling. |
| `frontend/src/features/auth/hooks/useSessionRevalidation.js` | Focus-based `me` revalidation without polling that would keep idle sessions alive. |
| `frontend/src/features/auth/components/UserSessionCard.jsx` | Displays the safe server identity, exact server role labels, and sign-out action. |
| `frontend/src/features/auth/lib/roles.js` | Normalizes role-list shape, strings, whitespace, and duplicates without mapping role meaning. |
| `frontend/src/services/api/baseApi.js` | Adds `credentials: 'include'`, session tags, and global `401` session clearing while excluding login failures. |
| `frontend/src/app/routes.js` | Adds the public `/login` route and wraps the workspace in `AuthGate`/`RequireAuth`. |
| `frontend/src/app/store.js` | Adds the auth reducer and optional preloaded state for deterministic tests. |
| `frontend/src/components/layout/AppLayout.jsx` | Adds focus revalidation and updates the workspace footer copy. |
| `frontend/src/components/layout/SidebarContent.jsx` | Replaces the foundation status card with current-session identity and sign-out. |
| `frontend/src/features/dashboard/pages/DashboardPage.jsx` | Updates workspace copy and status for the identity-enabled module. |
| `frontend/src/features/foundation/pages/FoundationPage.jsx` | Documents session-aware routing and the auth feature boundary. |
| `frontend/src/features/system/pages/SystemPage.jsx` | Removes the now-inaccurate statement that authentication does not exist. |
| `frontend/tests/features/authApi.test.js` | API request, cookie, CSRF header, role-response preservation, logout, and `401` tests. |
| `frontend/tests/features/LoginPage.test.jsx` | Form, loading, safe errors, throttling, disabled-account indistinguishability, CSRF rejection, destination restore, and no-storage tests. |
| `frontend/tests/features/AuthRouting.test.jsx` | Session restore, no-session redirect, unavailable retry, logout, and focus-expiry tests. |
| `frontend/tests/features/RoleContract.test.jsx` | Exact platform/organization/staff rendering, role-independent navigation, and STAFF sign-in regressions. |
| `frontend/tests/test-utils.jsx` | Platform-admin, organization-admin, and STAFF test preloads plus configurable auth states. |
| `frontend/tests/helpers/api.js` | Platform-admin, organization-admin, and STAFF auth fixtures and test helpers. |
| `frontend/README.md` | Authentication flow, bootstrap guidance, API integration, tests, and limitations. |

No backend source or configuration file was edited. No dependency was added.

## Consumed backend contract

| Method | Path | Frontend behavior |
|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Fetch a token immediately before login or logout. |
| `POST` | `/api/v1/auth/login` | Send `{ loginIdentifier, password }` plus `X-XSRF-TOKEN`. |
| `GET` | `/api/v1/auth/me` | Restore session state and revalidate on focus. |
| `POST` | `/api/v1/auth/logout` | Send `X-XSRF-TOKEN`; treat `204` as success. |

The safe identity response used by the UI is:

```json
{
  "id": 1,
  "loginIdentifier": "platform-admin@example.com",
  "displayName": "Platform Administrator",
  "roles": ["PLATFORM_ADMIN", "ORGANIZATION_ADMIN", "STAFF"]
}
```

The finalized role names are exactly `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and
`STAFF`. A response can contain one or more of those values; the UI displays the
values it receives without assigning capabilities or inferring a hierarchy.

Login was verified against the documented `200`, `400`, `401`, `403`, and `429`
status contract. The UI does not read backend message text for login failures,
so invalid credentials and disabled accounts remain indistinguishable. Role
values are displayed only; they do not authorize frontend behavior.

## Security and behavior decisions

- The CSRF token is kept only in memory for the request that needs it. The
  readable `XSRF-TOKEN` cookie is set by the backend but the frontend does not
  store it in browser storage.
- Password values exist only in component state while the form is mounted.
  `localStorage` and `sessionStorage` remain empty after login and logout.
- The protected-route boundary is navigation UX, not an authorization claim.
  Backend deny-by-default rules remain authoritative.
- Login and logout both fetch a fresh CSRF token rather than reusing a stale
  cached value.
- Login failure `401` is excluded from global session clearing. Other `401`
  responses clear session state and redirect through the route guard.
- Successful login resets RTK Query state before establishing the new identity,
  preventing cached data from a previous session from surviving account changes.
- Focus revalidation checks `/me` only when the window becomes visible; polling
  was rejected because it would keep the backend's 30-minute idle session alive.
- Role labels are opaque, server-assigned metadata. They do not affect routing,
  navigation, controls, or client-side permission checks; the backend remains
  the authorization authority. The prefixed `ROLE_*` authority strings are Spring
  internals, not identity-response values, and the UI assumes no role hierarchy.

## Verification results

### Frontend gates

Final commands, run after the last source change in revision 0.1.3B-R:

```text
npm run lint
  exit 0; no ESLint output

npm test
  Test Files  8 passed (8)
  Tests       37 passed (37)
  Duration    15.38s

npm run build
  2012 modules transformed
  built in 1.91s
  exit 0
```

The 37 tests include the original 13 shell/system tests, 20 session/CSRF/sign-in
and routing tests, and four role-contract regressions:

- direct preservation of `["PLATFORM_ADMIN", "ORGANIZATION_ADMIN", "STAFF"]`;
- exact multi-role rendering in the sidebar;
- navigation and sign-out that remain independent of the STAFF label;
- a complete STAFF sign-in flow through CSRF and login.

Existing coverage includes loading, success, invalid credentials,
disabled-account indistinguishability, throttling, CSRF rejection, network
failure, route destination preservation, session restore, no-session redirect,
retryable bootstrap failure, logout, and background `401` expiry. Storage remains
empty in the successful sign-in test.

### Live browser and backend integration

Two live runs were performed. The first predates the 0.1.3A-R role
reconciliation and the 0.1.3B-R role-label revision and used the legacy `ADMIN`
label; it remains session/CSRF/logout evidence. The second used the current
0.1.3A-R jar with Flyway V3 and verified the final three-role contract end to
end.

#### Historical session/CSRF run

A packaged Module 0.1.3A jar was run against a fresh temporary MySQL 8 schema.
The schema applied Flyway V1 and V2, and a temporary bootstrap administrator was
created. A dedicated Vite instance proxied `/api` to that backend. The temporary
account credentials were never written to the repository or printed; the
temporary schema, processes, logs, and credential file were removed afterward.

Observed browser flow:

```text
GET  /api/v1/auth/me (no session)              -> 401
UI redirect                                    -> /login
POST /api/v1/auth/csrf + /api/v1/auth/login    -> 200, session established
Protected workspace                            -> rendered
Identity and legacy ADMIN role                 -> rendered from /me response
Full reload at /system                         -> session restored, page loaded
Sign out (CSRF + logout)                       -> 204
UI                                             -> /login
GET  /api/v1/auth/me after logout              -> 401
```

Browser checks:

- `HOSTELOS_SESSION` was present and HttpOnly.
- `XSRF-TOKEN` was present and readable by same-origin JavaScript.
- `localStorage` and `sessionStorage` each had zero entries after login and after
  logout.
- The 401 at initial session restore and the 401 after logout were the only
  failed requests / console errors; both are expected authentication outcomes.
- Desktop and 390px mobile screenshots were inspected. The sign-in form, sidebar
  identity, and mobile navigation rendered without horizontal overflow.

The browser retained the invalid HttpOnly session cookie after logout until the
browser discarded it. The backend session itself was invalidated, as proven by
the subsequent `/me` `401`. The frontend cannot and does not attempt to modify an
HttpOnly cookie directly.

#### Final-contract live run

A current 0.1.3A-R jar was run against a disposable MySQL 8 schema migrated
through Flyway V3, with a dedicated Vite proxy. The temporary schema contained
exactly `ORGANIZATION_ADMIN`, `PLATFORM_ADMIN`, and `STAFF`; the environment
bootstrap account held `PLATFORM_ADMIN` only. A real browser then:

- signed in through CSRF-protected `/login`;
- received `200` from `/me` with `roles: ["PLATFORM_ADMIN"]`;
- rendered only `PLATFORM_ADMIN` in the sidebar, with no administration link;
- restored the same role after a full `/system` reload;
- signed out and received `401` from the next `/me` request;
- left both browser storages empty; the session cookie remained HttpOnly with
  `SameSite=Lax`, while the CSRF cookie remained readable.

The initial unauthenticated session check and the post-logout check produced the
only `401` responses. The temporary schema, bootstrap password, processes, logs,
and screenshot were removed afterward. The existing development backend and Vite
instance were not replaced.

A separate visual-only browser check intercepted `/me` with an identity carrying
all three role names. It confirmed exact label rendering, the informational role
note, no horizontal overflow at 1440x900 or 390x844, and no console errors. That
check supplements the final-contract live run above.

### Backend verification boundary

The backend test suite was not rerun by this frontend workstream. The current
backend jar was run for the final-contract live check against a disposable
schema, without modifying backend source, configuration, migrations, or tests.

## Repository state

- Branch: `feature/0.1.3-identity-authorization`.
- The frontend changes, 0.1.3B-R role alignment, shared documentation updates,
  and the uncommitted 0.1.3A/0.1.3A-R backend working tree remain uncommitted.
- No commit was created.
- Backend files were not modified by this module or revision.
- There is no CI workflow or committed browser automation.

## Known limitations and follow-ups

1. No administrative user-management API; bootstrap is the only account-creation
   path, and the frontend deliberately exposes no administration screen.
2. No role-based frontend capabilities are implemented because no corresponding
   backend capability exists. `ORGANIZATION_ADMIN` is a label only until
   organization membership and tenant-scoped enforcement exist; role labels
   remain informational.
3. No MFA, password reset, account recovery, email verification, or audit trail.
4. Session expiry is detected on focus and on API `401` responses, not by polling.
5. The invalid HttpOnly session cookie can remain in the browser after logout,
   though the server rejects it.
6. No CI pipeline or checked-in browser test.
7. CORS remains intentionally unconfigured; only the same-origin Vite proxy
   topology is verified.
8. The backend login limiter is process-local and the packaging/repository
   governance gaps recorded by Module 0.1.3A remain.

## References

- `frontend/README.md` - setup, authentication flow, API, tests, limitations.
- `Backend/src/main/java/com/rahul/HostelOs/identity/RoleName.java` - finalized roles.
- `Backend/src/main/resources/db/migration/V3__reconcile_platform_admin_role.sql` - role migration.
- `Backend/src/main/java/com/rahul/HostelOs/auth/AuthUserResponse.java` - identity role response.
- `Backend/docs/API.md` - auth API contract.
- `docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md` - accepted session/CSRF design.
- `Backend/docs/HOSTELOS_MODULE_0.1.3A_REPORT.md` - backend evidence.
- `docs/CURRENT_STATE.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`,
  `docs/IMPLEMENTATION_LOG.md` - updated shared status.
