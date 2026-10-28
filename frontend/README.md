# HostelOS Frontend

React 19, Vite 8, Tailwind CSS 4, Redux Toolkit, and RTK Query frontend for
HostelOS.

## Requirements

- Node.js `^22.13.0`, `^24.0.0`, or `>=26.0.0`
- npm
- The HostelOS backend running on port `8080` by default
- JDK 17 for the backend Maven wrapper

## Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173/`. The system-information page is at
`http://localhost:5173/system`.

## Start the backend

The backend lives in `../Backend`. It needs a local MySQL database and the
environment variables documented in `../Backend/.env.example`. Copy that file
to `../Backend/.env` for local use and never commit the resulting file.

```bash
cd ../Backend
JAVA_HOME="C:/Users/RAHUL/.jdks/ms-17.0.16" \
  PATH="C:/Users/RAHUL/.jdks/ms-17.0.16/bin:$PATH" \
  ./mvnw spring-boot:run
```

Or start the packaged jar from the `Backend` directory so its optional `.env`
import resolves:

```bash
cd ../Backend
JAVA_HOME="C:/Users/RAHUL/.jdks/ms-17.0.16" \
  "C:/Users/RAHUL/.jdks/ms-17.0.16/bin/java.exe" \
  -jar target/HostelOs-0.0.1-SNAPSHOT.jar
```

The backend health endpoint is `http://localhost:8080/actuator/health`.

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

`tests` uses `.env.test` with an absolute API origin because Node's `Request`
constructor requires one. The application itself keeps the relative `/api` base
URL in development and production.

## API integration

The shared RTK Query client is `src/services/api/baseApi.js`. The first live
endpoint is defined in `src/features/system/api/systemApi.js`:

```text
GET /api/v1/system/info
```

Actual backend response verified on 2026-10-28:

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

The accepted contract uses `status: "AVAILABLE"`. The earlier integration brief
expected `"UP"`; that value is retained only as historical context. The backend
response is treated as authoritative; the frontend displays the returned value
unchanged and does not map it to `UP`. The status describes this metadata
endpoint's availability, not dependency or platform health.

The UI validates that all three fields are non-empty strings and shows an error
instead of inventing values when they are missing. The page does not claim
platform-wide health from this metadata endpoint alone.

### Current integration status

The real development-proxy path has been verified:

```text
GET http://localhost:8080/api/v1/system/info -> 200
GET http://localhost:5173/api/v1/system/info -> 200 through the Vite proxy
```

The backend permits only `GET` on this path; other methods return JSON `401`.
Mocked tests cover loading, success, malformed data, and error/retry behavior.
`AVAILABLE` is the accepted contract; the earlier `UP` wording is historical.

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

The tests use deterministic mocked `fetch` responses for API success, loading,
malformed data, and error/retry behavior. Existing routing and navigation tests
remain in place.

For a real development-proxy smoke test:

1. Start the backend on port `8080`.
2. Start `npm run dev` and open `http://localhost:5173/system`.
3. Confirm the browser request goes to `/api/v1/system/info` through Vite.
4. From a terminal:

```bash
curl -i http://localhost:5173/api/v1/system/info
```

Mocked tests and real proxy evidence are separate. Do not report the mocked
success tests as a live integration result.

## Project structure

```text
src/app                  Providers, router, store, and route definitions
src/components/ui        Shared UI primitives
src/components/layout    Application shell and navigation
src/components/feedback  Loading, error, and empty states
src/features/system      System-information API, validation, and page
src/features/foundation  Foundation documentation page
src/services/api         Shared RTK Query base API
src/hooks                Reusable React hooks
src/lib                  Environment configuration
src/styles               Global styles and design tokens
tests                    Test setup and application suites
```

## Known limitations

- The earlier integration brief expected `status: "UP"`; `AVAILABLE` is now the
  accepted contract and the frontend preserves that value.
- There is no authentication or protected business workflow.
- CORS is intentionally not configured; local development relies on the Vite
  proxy.
- There is no CI pipeline in this workspace.
