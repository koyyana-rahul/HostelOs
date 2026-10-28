# ADR-0001: Identity and Authentication Foundation

Date: 2026-10-28
Status: Accepted for Module 0.1.3A; amended by Module 0.1.3A-R
Decision owner: HostelOS backend

## Context

HostelOS is currently a modular monolith with a Vite React SPA and a Spring
Boot backend. Development uses a same-origin Vite proxy: the browser calls
relative `/api/...` paths and Vite forwards them to Spring Boot. There is no
CORS configuration and no browser-visible API origin distinction.

Module 0.1.2 exposes one public read-only endpoint:

```text
GET /api/v1/system/info
```

All other application routes are denied. Module 0.1.3A adds the first identity
foundation so later modules can protect resident, hostel, billing, and
operations workflows.

## Threat model

### Assets

- User login identifiers and display names.
- Password verifiers, not recoverable plaintext passwords.
- Session identifiers, CSRF tokens, and authentication state.
- Role assignments and account status.
- Future resident, hostel, billing, and operations data.

### Adversaries and failure modes

| Threat | Impact | Required control |
|---|---|---|
| Credential stuffing or password guessing | Account takeover | Strong password hashing, generic failures, rate limiting, future MFA |
| Account enumeration | Discovery of valid identities | Uniform login failure response and timing-safe authentication |
| CSRF | Unauthorized state changes through a victim session | Same-site cookies, CSRF token required for authenticated mutations and login |
| Session theft through XSS or network interception | Session impersonation | HttpOnly cookies, Secure cookies in production, short idle timeout, no token storage in browser storage |
| Session fixation | Attacker-selected session identifier reuse | Session identifier rotation on login |
| Stolen password or stale session after disablement | Continued access | Server-side status check on authenticated requests and session invalidation on logout/disablement |
| Password-hash disclosure | Offline cracking | BCrypt, no hash in API responses or logs, database access least privilege |
| Accidental public endpoint | Data exposure | Explicit allowlist and deny-by-default authorization |
| Default or seeded administrator | Immediate compromise | No default account; one-time environment-driven bootstrap only |
| Brute-force denial of service | Lockout of users | Per-identifier/per-IP throttling with bounded state; distributed limiter later |
| Repository secret leakage | Credential compromise | Environment/secret manager only; `.env` ignored; no passwords printed |

### Out of scope

- MFA, password reset, email verification, and account recovery.
- Social login, SAML, OIDC, and external identity providers.
- Tenant, organization, hostel, or property-scoped authorization.
- Administrative user-management screens.
- Distributed sessions, shared rate-limit storage, and SIEM/audit-log
  integration.
- Frontend authentication UI changes.

## Authentication options considered

### Option A: Browser session cookie backed by Spring Security sessions

The server authenticates once and stores the security context in an
`HttpSession`. The browser receives an HttpOnly session cookie. Mutating
requests require a CSRF token.

Advantages:

- Fits the current same-origin Vite proxy and a modular monolith.
- Logout and disablement can revoke server-side state immediately.
- Session identifiers are not readable by JavaScript when HttpOnly.
- CSRF protection is a supported Spring Security concern.
- No access/refresh token rotation code or token storage in the SPA.
- Simpler operational model than a token service for the current single
  deployment boundary.

Disadvantages:

- Server-side session state; horizontal scaling requires sticky sessions or a
  shared session store.
- CSRF token bootstrap and header handling must be implemented.
- Browser/mobile clients outside the same-origin model need additional design.
- A stolen cookie can be replayed until expiry or invalidation.

### Option B: Short-lived JWT access token plus refresh token

The client holds a short-lived access token and a longer-lived refresh token.
Refresh tokens would be rotated and ideally stored in an HttpOnly cookie.

Advantages:

- Access tokens can be validated without a database lookup.
- Useful for independent services and non-browser clients.
- Can carry scoped claims if designed carefully.

Disadvantages:

- Logout and disablement do not revoke already-issued access tokens without
  server-side denylist/session state, which reintroduces state.
- Refresh-token rotation, reuse detection, device binding, and storage add
  significant complexity.
- Browser storage increases XSS impact unless refresh tokens use cookies, which
  brings back CSRF concerns.
- The current monolith has no independent service boundary that justifies the
  operational complexity.
- Token claims can become stale; server-side authorization still needs fresh
  identity/role data.

### Decision

Use **Option A: server-side Spring Security sessions with an HttpOnly cookie**.

The current deployment is one same-origin browser application. Immediate
revocation, simple logout, no client-side token storage, and a smaller attack
surface are more valuable than stateless token scaling. Future independent
services should integrate through an explicit OIDC/opaque-token boundary rather
than retrofitting JWT handling into this monolith.

## Identity model

### Tables

`user_accounts`:

- Surrogate `id`.
- `login_identifier`: unique, normalized identifier (Unicode NFKC, trimmed,
  lower-cased).
- `display_name`: non-sensitive human-readable name.
- `password_hash`: BCrypt verifier; never returned by an API.
- `status`: `ACTIVE` or `DISABLED`.
- `version`: optimistic locking version.
- `created_at`, `updated_at`: UTC timestamps.

`roles`:

- `id`.
- `name`: `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, or `STAFF`.
- `description`.

`user_roles`:

- Composite key `(user_id, role_id)`.
- Foreign keys to both tables.
- Index on `role_id`.

Roles are seeded as data. No user account is seeded.

### Authorization model

Spring Security authorities are derived only from server-side role assignments.
Client-supplied role names, user IDs, or status values are never trusted. The
login contract accepts only a login identifier and a password; an extra role
field in a request is not read.

Role semantics and permission boundaries:

| Role | Authority | Scope | Current permission boundary |
|---|---|---|---|
| `PLATFORM_ADMIN` | `ROLE_PLATFORM_ADMIN` | Platform-wide | Platform identity and privileged configuration. It is not an organization role. |
| `ORGANIZATION_ADMIN` | `ROLE_ORGANIZATION_ADMIN` | Organization, deferred | Reserved for future organization administration. No organization membership or tenant model exists, so no tenant-scoped permission is claimed or enforced. It must never authorize platform-wide endpoints. |
| `STAFF` | `ROLE_STAFF` | Operational | Authenticated operational staff workflows. It does not authorize platform or organization administration. |

There is no implicit role hierarchy. A platform administrator is not
automatically an organization administrator or staff member, and an
organization administrator is never treated as a platform administrator.
Endpoints must state the exact authority they require. No current business API
uses these roles yet; authentication and method-security probes verify the
boundaries.

Module 0.1.3A-R does not add tenant, hostel, or property scopes. Future
membership tables should associate an authenticated user with an
organization/hostel and should be checked independently of the global role. A
user ID from the session, not a request body, must be the source of identity.
Organization-scoped authorization remains deferred until that membership model
exists.

### Role reconciliation decision (Module 0.1.3A-R)

The original `ADMIN` role was the only privileged role and was the role assigned
by the administrator bootstrap. At that time no organization, membership, or
tenant boundary existed. The role therefore represented authority over the
whole platform, not over an organization. Keeping the name `ADMIN` would allow
an ordinary organization administrator to be mistaken for a platform
administrator.

Decision: migrate the existing `ADMIN` role in place to `PLATFORM_ADMIN`, add
`ORGANIZATION_ADMIN` as a distinct seeded role, and keep `STAFF` unchanged.

Exact mapping:

| Before | After | Assignment handling |
|---|---|---|
| `ADMIN` | `PLATFORM_ADMIN` | The role row is renamed in place, so every existing `user_roles` row keeps the same role ID and remains effective. |
| none | `ORGANIZATION_ADMIN` | New seeded role with no user assignments. |
| `STAFF` | `STAFF` | Unchanged role row, assignments, and authority name. |

Consequences:

- Every existing `ADMIN` account becomes a platform-wide administrator. This is
  the existing meaning of the role, not an expansion of scope.
- No existing account is automatically granted `ORGANIZATION_ADMIN` or `STAFF`.
- External consumers that compare the role string `ADMIN` must use
  `PLATFORM_ADMIN`; the `/me` response and Spring authority are
  `PLATFORM_ADMIN` and `ROLE_PLATFORM_ADMIN`.
- Existing sessions retain their old authorities until the next login because
  roles are cached in the session principal. Account status is still rechecked
  on every authenticated request.
- No identity rows or user-role assignments are deleted.

The bootstrap environment variable names are unchanged for deployment
compatibility, but the bootstrap account now receives only `PLATFORM_ADMIN`, and
only when `HOSTELOS_BOOTSTRAP_ADMIN_ENABLED=true` and all required values are
present.

Status is enforced server-side:

- Disabled users cannot authenticate.
- An existing session is invalidated when its account is no longer active.
- Account status is not accepted from the browser.

## Session and CSRF policy

- Session cookie name: `HOSTELOS_SESSION`.
- HttpOnly: yes.
- Secure: enabled in the `prod` profile, disabled for local HTTP development.
- SameSite: `Lax`.
- Idle timeout: 30 minutes.
- URL session tracking disabled.
- Session identifier rotated after successful login.
- Logout invalidates the server session.
- No remember-me and no concurrent-session registry in this module.
- CSRF: cookie-backed token (`XSRF-TOKEN`) with `X-XSRF-TOKEN` header.
- A safe CSRF bootstrap endpoint returns the token for the SPA.
- CSRF is required for login and for authenticated state-changing requests.
  Unauthenticated protected requests still receive the established JSON `401`
  instead of a CSRF `403`, preserving Module 0.1.2 behavior.
- The CSRF cookie is not HttpOnly because a same-origin SPA may need to read it.
  The token is not a password or a session credential.

## Authentication endpoints

| Method | Path | Authentication | CSRF | Purpose |
|---|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Public | Not applicable | Bootstrap a CSRF token |
| `POST` | `/api/v1/auth/login` | Public | Required | Authenticate and create a session |
| `POST` | `/api/v1/auth/logout` | Public/idempotent | Required when authenticated | Invalidate the current session |
| `GET` | `/api/v1/auth/me` | Authenticated | Not applicable | Return the current identity and roles |

Login accepts JSON:

```json
{
  "loginIdentifier": "user@example.com",
  "password": "user-supplied-password"
}
```

Failed authentication returns a generic `401` with no account-existence hint.
Throttled attempts return `429` with a generic retry-later message. Validation
errors use the existing shared `400` error shape.

## Brute-force policy

The initial limiter is in-memory, keyed by normalized login identifier and the
request remote address, with five failures per fifteen minutes. It is bounded
and prunes expired windows. It is sufficient for a single development
instance; a production multi-instance deployment must use a shared limiter or
edge rate limiting. `X-Forwarded-For` is not trusted until a trusted reverse
proxy is explicitly configured.

## Bootstrap strategy

No administrator is created by default. A one-time startup bootstrap is
available only when all of the following environment variables are supplied:

```text
HOSTELOS_BOOTSTRAP_ADMIN_ENABLED=true
HOSTELOS_BOOTSTRAP_ADMIN_LOGIN
HOSTELOS_BOOTSTRAP_ADMIN_DISPLAY_NAME
HOSTELOS_BOOTSTRAP_ADMIN_PASSWORD
```

The password must be at least 16 characters. When explicitly enabled, the
runner creates one active `PLATFORM_ADMIN` account if the normalized login does
not already exist. It never edits an existing account, assigns no other role,
and logs neither the password nor the hash. Operators should unset the bootstrap
variables after the first successful startup and rotate the bootstrap credential
according to the deployment's credential policy.

## Migration and rollback

- `V2__identity_foundation.sql` added the three tables, constraints, indexes, and
  the original role seed data.
- `V3__reconcile_platform_admin_role.sql` is the forward-only Module 0.1.3A-R
  migration. It drops the old role-name check constraint, renames `ADMIN` to
  `PLATFORM_ADMIN` in place, inserts `ORGANIZATION_ADMIN`, updates role
  descriptions, and installs the new three-role check constraint.
- The rename preserves the `roles.id` value, so all existing `user_roles` rows
  remain valid. No user, role-assignment, or role row is deleted.
- Do not edit `V2` after it has been applied. Do not edit `V3` after it has been
  applied in any shared environment; add `V4` instead.
- Do not use `flyway clean`, schema resets, or `ddl-auto=update`.
- Take a database backup before applying `V3` in production.
- Application rollback is safe only against the reconciled role names; the
  application must not be rolled back to a build that expects `ADMIN` while
  `V3` remains applied. Database rollback requires an explicit restore of the
  pre-migration backup; there is no automatic down migration.
- The migration must be validated on H2 (automated tests) and MySQL (manual
  deployment verification) because H2 MySQL mode is not a complete substitute.
  Automated coverage includes upgrading a V2 database that already has identity
  and role-assignment rows.

## Unresolved risks

1. No MFA, password reset, or account recovery.
2. No distributed session store, concurrent-session control, or shared rate
   limiter.
3. No absolute session lifetime beyond the idle timeout.
4. No audit trail for login success, failure, logout, or status changes.
5. No administrative user-management API; bootstrap is the only account-creation
   mechanism until an approved admin workflow exists.
6. Role changes are not reflected in an existing session until re-login; account
   disablement is checked on each authenticated request.
7. `ORGANIZATION_ADMIN` exists as a role only. Organization membership, tenant
   boundaries, and organization-scoped enforcement are not implemented, so
   assigning this role grants no organization API access today.
8. H2 and MySQL behavior must be kept aligned; MySQL integration testing remains
   manual.
9. CSRF and cookie behavior through a production reverse proxy must be verified
   when that topology is implemented.
10. Existing `ADMIN` assignments become platform-wide administrator authority on
    upgrade; operators must review privileged accounts before applying `V3`.
