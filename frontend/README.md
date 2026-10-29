# HostelOS Frontend

React 19, Vite 8, Tailwind CSS 4, Redux Toolkit, and RTK Query frontend for
HostelOS. The workspace is protected by a backend-managed session and includes a
responsive sign-in flow.

## Requirements

- Node.js `^22.13.0`, `^24.0.0`, or `>=26.0.0`
- npm
- The HostelOS backend running on port `8080` by default
- JDK 17 for the backend Maven wrapper
- A provisioned HostelOS account; no default account is seeded

## Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173/`. Unauthenticated visitors are redirected to
`/login`. The system-information page remains at `/system` and requires a valid
session in the workspace UI.

## Start the backend

The backend lives in `../Backend`. It needs a local MySQL database and the
environment variables documented in `../Backend/.env.example`. Copy that file
to `../Backend/.env` for local use and never commit the resulting file.

No administrator is seeded by default. For initial local access, configure all
of the following through backend environment variables and then start the
backend:

```text
HOSTELOS_BOOTSTRAP_ADMIN_ENABLED=true
HOSTELOS_BOOTSTRAP_ADMIN_LOGIN
HOSTELOS_BOOTSTRAP_ADMIN_DISPLAY_NAME
HOSTELOS_BOOTSTRAP_ADMIN_PASSWORD
```

The bootstrap password must be at least 16 characters. Disable or unset these
variables after provisioning. Do not put these values in frontend environment
files or source code.

```bash
cd ../Backend
export JAVA_HOME=/path/to/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spring-boot:run
```

Or start the packaged jar from the `Backend` directory so its optional `.env`
import resolves:

```bash
cd ../Backend
java -jar target/HostelOs-0.0.1-SNAPSHOT.jar
```

The backend health endpoint is `http://localhost:8080/actuator/health`.

## Authentication

The frontend uses the backend's server-side session model. It does not use JWT,
and it never stores passwords or access tokens in `localStorage` or
`sessionStorage`.

Session flow:

1. On application load, `AuthGate` requests `GET /api/v1/auth/me` to restore the
   server session.
2. If no session exists, protected routes redirect to `/login` and preserve the
   requested internal path.
3. Login first requests `GET /api/v1/auth/csrf`, then posts
   `{ loginIdentifier, password }` to `POST /api/v1/auth/login` with the
   server-provided CSRF header and cookies.
4. The backend sets the HttpOnly `HOSTELOS_SESSION` cookie. The frontend keeps
   only the safe identity response in Redux.
5. When the window regains focus, the current identity is revalidated. A `401`
   from any API request clears local session state and returns the user to the
   sign-in page.
6. Logout requests a fresh CSRF token, posts to `POST /api/v1/auth/logout`, clears
   RTK Query and Redux auth state, and returns to `/login`.

### Role contract

The backend's finalized role names are `PLATFORM_ADMIN`,
`ORGANIZATION_ADMIN`, and `STAFF`. Login and `GET /api/v1/auth/me` return those
exact strings in the `roles` array. The sidebar renders each server value as
informational identity metadata.

Roles do not control frontend navigation, show or hide features, or grant
actions. There are no platform- or organization-administration screens in this
frontend, and a role label does not imply a capability that the backend does not
expose. `ORGANIZATION_ADMIN` is a role definition only; organization membership
and tenant-scoped enforcement do not exist yet. Protected routes only manage
session navigation; the backend remains authoritative and denies unauthorized
requests.

Spring Security internally derives `ROLE_PLATFORM_ADMIN`,
`ROLE_ORGANIZATION_ADMIN`, and `ROLE_STAFF` authorities from the server-side
assignments, but those prefixed authority strings are not part of the identity
API contract. The forward-only Flyway `V3` migration renames the original
`ADMIN` role to `PLATFORM_ADMIN` in place and adds `ORGANIZATION_ADMIN`.

## Configuration

| Variable | Default | Used by |
|---|---|---|
| `VITE_APP_NAME` | `HostelOS` | Browser-visible application name |
| `VITE_APP_VERSION` | `0.1.1` | Browser-visible version label |
| `VITE_API_BASE_URL` | `/api` | RTK Query base URL |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Vite development proxy target |

Create `.env.local` to override values locally. Only `VITE_`-prefixed values
reach the browser, and those values are public. Do not put secrets in them.

The Vite development server proxies `/api` to `VITE_API_PROXY_TARGET`, so
browser requests remain same-origin and do not require CORS. Do not add
permissive CORS settings in the frontend.

The `tests` environment uses `.env.test` with an absolute API origin because
Node's `Request` constructor requires one. The application itself keeps the
relative `/api` base URL in development and production.

## API integration

The shared RTK Query client is `src/services/api/baseApi.js`. It sends cookies
with every request and clears local session state when an authenticated request
returns `401`.

Current endpoints:

| Method | Path | Frontend use |
|---|---|---|
| `GET` | `/api/v1/system/info` | `/system` metadata page |
| `GET` | `/api/v1/auth/csrf` | CSRF bootstrap before login/logout |
| `POST` | `/api/v1/auth/login` | Sign-in form |
| `POST` | `/api/v1/auth/logout` | Sidebar sign-out action |
| `GET` | `/api/v1/auth/me` | Session restore and focus revalidation |

The accepted system metadata response is:

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

The UI validates that all three fields are non-empty strings and shows an error
instead of inventing values when they are missing. The page does not claim
platform-wide health from this metadata endpoint alone.

The sign-in form handles validation, pending submission, invalid credentials,
disabled-account failures, CSRF rejection, throttling, and network/unavailable
errors with safe messages. The backend's disabled-account response is
intentionally indistinguishable from an invalid login.

## Scripts

```bash
npm run dev        # start the Vite development server
npm test           # run the test suite once
npm run test:watch # watch tests during development
npm run build      # create the production build
npm run lint       # check the codebase with ESLint
npm run preview    # serve the production build
```

## Tests

```bash
npm test
```

The deterministic test suite covers:

- existing shell, navigation, not-found, and system-information behavior;
- CSRF bootstrap, login request headers, cookies, and logout requests;
- session restore success, no-session redirect, and unavailable-backend retry;
- protected-route destination preservation;
- login validation, loading, invalid credentials, disabled-account
  indistinguishability, throttling, CSRF rejection, and network failure;
- logout clearing the shell and background `401` session expiry;
- the finalized `PLATFORM_ADMIN`/`ORGANIZATION_ADMIN`/`STAFF` identity response,
  exact role-label rendering, STAFF sign-in, and navigation that is independent
  of role labels.

For a real development-proxy smoke test:

1. Start the backend on port `8080` with a provisioned account.
2. Start `npm run dev` and open `http://localhost:5173/`.
3. Sign in and confirm the protected workspace renders.
4. Reload `/system` and confirm the session is restored.
5. Sign out and confirm the app returns to `/login` and a subsequent
   `GET /api/v1/auth/me` returns `401`.

Mocked tests and real proxy evidence are separate. Do not report the mocked
success tests as a live integration result.

## Project structure

```text
src/app                  Providers, router, store, and route definitions
src/components/ui        Shared UI primitives
src/components/layout    Application shell and navigation
src/components/feedback  Loading, error, and empty states
src/features/auth        Session state, auth API, sign-in, roles, and route guards
src/features/system      System-information API, validation, and page
src/features/foundation  Foundation documentation page
src/services/api         Shared RTK Query base API
src/hooks                Reusable React hooks
src/lib                  Environment configuration
src/styles               Global styles and design tokens
tests                    Test setup and application suites
```

## Known limitations

- There is no administrative user-management API; bootstrap is the only
  account-creation path.
- MFA, password reset, account recovery, and email verification are not
  implemented.
- CORS is intentionally not configured; local development relies on the Vite
  proxy.
- Session-expiry revalidation occurs on window focus and API `401` responses.
  There is no continuous polling because that would keep server sessions alive.
- The HttpOnly session cookie may remain in the browser after logout until the
  browser discards it, but the backend session is invalidated and `/me` returns
  `401`.
- There is no CI pipeline or committed automated browser test in this workspace.
