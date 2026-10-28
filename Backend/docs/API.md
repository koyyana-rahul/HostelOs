# HostelOS Backend API

Last verified: 2026-10-28 (Module 0.1.3A-R)

## Development access

| Path | URL |
|---|---|
| Backend direct | `http://localhost:8080` |
| Vite development proxy | `http://localhost:5173/api` |

The frontend Vite server proxies `/api` to `http://localhost:8080` by default.
The target can be overridden with `VITE_API_PROXY_TARGET` in the frontend
environment. No CORS configuration exists; development is same-origin through
the proxy.

## Authentication overview

Module 0.1.3A adds server-side session authentication:

- `HOSTELOS_SESSION` is an HttpOnly session cookie.
- The session is created by `POST /api/v1/auth/login`.
- The session identifier is rotated on successful login.
- The idle timeout is 30 minutes; URL session tracking is disabled.
- The cookie is `SameSite=Lax` and Secure under the `prod` profile.
- CSRF uses the `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` request header.
- The CSRF cookie is readable by same-origin JavaScript; it is not a password or
  session credential.
- `POST /api/v1/auth/login` requires CSRF protection.
- Authenticated state-changing requests require CSRF protection.
- Unauthenticated protected requests return the established JSON `401`, not a
  CSRF `403`.

### CSRF flow

1. Call `GET /api/v1/auth/csrf`.
2. Read the `XSRF-TOKEN` cookie or the `token` field in the response.
3. Send the value in `X-XSRF-TOKEN` on `POST` requests.
4. Keep and send cookies on subsequent requests.

## `GET /api/v1/auth/csrf`

Returns a CSRF token and the header name expected by the backend.

- Authentication: none
- HTTP status: `200 OK`
- Response content type: `application/json`

```json
{
  "headerName": "X-XSRF-TOKEN",
  "token": "csrf-token-value"
}
```

The response also sets the `XSRF-TOKEN` cookie with `SameSite=Lax`. The cookie is
Secure under the `prod` profile.

## `POST /api/v1/auth/login`

Authenticates a user and creates a server-side session.

- Authentication: none
- CSRF: required
- Request content type: `application/json`
- Success status: `200 OK`
- Success body: safe identity data; never includes a password or hash

Request:

```json
{
  "loginIdentifier": "user@example.com",
  "password": "user-supplied-password"
}
```

Success response:

```json
{
  "id": 1,
  "loginIdentifier": "user@example.com",
  "displayName": "HostelOS Administrator",
  "roles": ["PLATFORM_ADMIN"]
}
```

Login identifiers are normalized with Unicode NFKC, trimming, and lower-casing.
The `loginIdentifier` request field is required, max 254 characters. The password
is required, max 200 characters at the DTO boundary; the service enforces a
maximum of 72 UTF-8 bytes for BCrypt.

Responses:

| Status | Meaning |
|---|---|
| `200` | Authentication succeeded; session created and rotated |
| `400` | Request validation failed |
| `401` | Invalid credentials; no account-existence hint |
| `403` | CSRF token missing or invalid |
| `429` | Too many failed attempts; retry later |

## `GET /api/v1/auth/me`

Returns the current authenticated identity.

- Authentication: required
- CSRF: not applicable
- HTTP status: `200 OK` when authenticated, `401` otherwise

```json
{
  "id": 1,
  "loginIdentifier": "user@example.com",
  "displayName": "HostelOS Administrator",
  "roles": ["PLATFORM_ADMIN"]
}
```

The response contains only safe identity fields. Password hashes and account
status are not exposed. Role values are server-assigned; the login request has no
role field, and any extra role field sent by a client is ignored.

## Role model

| Role | Spring authority | Scope | Current use |
|---|---|---|---|
| `PLATFORM_ADMIN` | `ROLE_PLATFORM_ADMIN` | Platform-wide | Platform identity and privileged configuration; bootstrap administrator role |
| `ORGANIZATION_ADMIN` | `ROLE_ORGANIZATION_ADMIN` | Organization, deferred | Reserved for future organization administration; no membership or tenant enforcement exists yet |
| `STAFF` | `ROLE_STAFF` | Operational | Operational staff workflows |

There is no implicit hierarchy. A platform administrator is not automatically an
organization administrator or staff member. An organization administrator is
never treated as a platform administrator. Organization-scoped authorization is
deferred until organization membership and tenant boundaries exist; the role
alone grants no organization API access.

## `POST /api/v1/auth/logout`

Invalidates the current server-side session.

- Authentication: public at the authorization layer; idempotent
- CSRF: required when authenticated
- Success status: `204 No Content`
- A subsequent `GET /api/v1/auth/me` returns `401`

## Login throttling

The initial limiter is in-memory and scoped to one process. It keys attempts by
normalized login identifier plus `request.getRemoteAddr()`.

- Threshold: 5 failures
- Window: 15 minutes
- Throttled response: generic `429`
- State is bounded to 10,000 keys and expired windows are pruned
- `X-Forwarded-For` is not trusted

Production multi-instance deployments require a shared limiter or edge rate
limiting.

## Administrator bootstrap

No administrator is seeded by default. A one-time startup bootstrap is available
only when all values are supplied:

```text
HOSTELOS_BOOTSTRAP_ADMIN_ENABLED=true
HOSTELOS_BOOTSTRAP_ADMIN_LOGIN
HOSTELOS_BOOTSTRAP_ADMIN_DISPLAY_NAME
HOSTELOS_BOOTSTRAP_ADMIN_PASSWORD
```

Requirements and behavior:

- Password minimum: 16 characters
- Normalized login already exists: no changes are made
- New account: active `PLATFORM_ADMIN` only
- Roles are never accepted from the request; the bootstrap assigns the platform
  role server-side
- Passwords and hashes are never logged
- Unset bootstrap variables after provisioning

`Backend/.env.example` contains disabled, blank placeholders. A real `.env` must
not be committed.

## Identity persistence

Flyway migration `V2__identity_foundation.sql` creates:

| Table | Purpose |
|---|---|
| `roles` | Server-assigned roles; originally seeded `ADMIN` and `STAFF` |
| `user_accounts` | Normalized login, display name, BCrypt hash, status |
| `user_roles` | User-to-role assignments |

`V3__reconcile_platform_admin_role.sql` then:

- Renames `ADMIN` to `PLATFORM_ADMIN` in place, preserving the role ID and every
  existing `user_roles` assignment.
- Seeds `ORGANIZATION_ADMIN` with no user assignments.
- Updates role descriptions and replaces the role-name check constraint with
  `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF`.
- Deletes no users, assignments, or role rows.

Account status is `ACTIVE` or `DISABLED`. Disabled accounts cannot authenticate,
and an active session is discarded by the active-account filter when its account
is disabled. Role changes take effect on the next login. Existing sessions keep
their previous authority names until re-login after the V3 migration.

The migrations are additive and forward-only. Do not edit an applied migration,
and do not use Flyway clean/reset, `ddl-auto=update`, or destructive resets.

## `GET /api/v1/system/info`

Returns stable, non-sensitive application metadata. The endpoint does not query
the database.

- Authentication: none
- HTTP status: `200 OK`
- Response content type: `application/json`

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

`status` describes the availability of this endpoint only. It is not a claim that
MySQL, Flyway, or every application dependency is healthy. Dependency health is
reported separately by `/actuator/health`.

## Authorization policy

Public application routes:

- `GET /api/v1/system/info`
- `GET /api/v1/auth/csrf`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`

Authenticated application routes:

- `GET /api/v1/auth/me`

Everything else is denied by default. Infrastructure endpoints such as health,
OpenAPI, Swagger UI, and `/error` remain public according to the existing
profile policy. No broad `/api/**` permit was added.

## OpenAPI

The generated OpenAPI document includes the `sessionAuth` cookie security scheme
for the session-authenticated endpoints.

- `/v3/api-docs`
- `/swagger-ui.html`

OpenAPI and Swagger UI are enabled in the default development profile and
disabled by the `prod` profile.

## Error contract

Application errors use the shared `ApiError` shape:

```json
{
  "timestamp": "2026-10-28T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required",
  "path": "/api/v1/auth/me",
  "violations": []
}
```

Validation failures return `400` with field violations. Method-security denials
return JSON `403`. Unexpected errors return a generic JSON `500` without stack
traces or internal details.

## CORS decision

Development integration is same-origin through the Vite `/api` proxy. No CORS
configuration, wildcard origin, or `@CrossOrigin` exception was added.

A separately hosted frontend and backend would require an explicit
production-origin decision and strict origin allowlisting. That decision remains
deferred.

## Frontend integration boundary

The frontend consumes this contract for login, logout, CSRF bootstrap, session
restore, and identity display. It treats role values as informational metadata
and does not derive authorization from them. The backend remains the
authorization boundary. The in-repo frontend fixtures and role-contract tests
are aligned to `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF` through
Module 0.1.3B-R.

## Related health endpoint

`GET /actuator/health` reports application and dependency health. The system
information endpoint deliberately does not duplicate that responsibility.
