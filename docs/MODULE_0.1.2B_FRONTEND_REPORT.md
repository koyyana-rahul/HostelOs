# Module 0.1.2B - Frontend API Integration Report

> Finalization: Module 0.1.2C independently verified the cross-stack browser
> flow, and backend stabilization resolved the technical test-gate hold.
> `AVAILABLE` is the accepted contract. Technical release criteria are
> satisfied; Git/tracked-secret governance remains unresolved. See
> `docs/INTEGRATION_QA_0.1.2.md` and
> `Backend/docs/HOSTELOS_MODULE_0.1.2A_SECURITY_TEST_STABILIZATION.md`.

Date: 2026-10-28
Scope: frontend integration of `GET /api/v1/system/info`
Result: implemented and verified; the historical `UP` wording is now resolved
by the accepted `AVAILABLE` contract. Repository/CI limitations remain.

## Outcome

The frontend now has one real read-only API integration. The `/system` page
calls `GET /api/v1/system/info` through the existing shared RTK Query base API,
validates the required metadata, and renders loading, success, error, and retry
states.

The authoritative backend response observed at runtime was:

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

The original integration brief expected `status: "UP"`. The frontend preserves
the backend value (`AVAILABLE`) instead of transforming it. That historical
mismatch was resolved during finalization by accepting `AVAILABLE` as the
contract; the backend was not modified.

## Frontend changes

| File | Change |
|---|---|
| `frontend/src/features/system/api/systemApi.js` | New `getSystemInfo` endpoint injected into `baseApi`. |
| `frontend/src/features/system/lib/systemInfo.js` | Response validation, safe error mapping, and status tone mapping. |
| `frontend/src/features/system/pages/SystemPage.jsx` | New `/system` page with metadata, status badge, loading, error, and retry states. |
| `frontend/src/app/routes.js` | Added lazy `/system` route. |
| `frontend/src/components/layout/navigation.js` | Added the System navigation item. |
| `frontend/tests/features/systemApi.test.js` | Four API integration tests. |
| `frontend/tests/features/SystemPage.test.jsx` | Five page and navigation tests. |
| `frontend/tests/helpers/api.js` | Shared system-info fixture and fetch helpers; fixture uses `AVAILABLE`. |
| `frontend/tests/setup.js` | Restores stubbed globals after each test. |
| `frontend/.env.test` | Test-only absolute API base URL for Node `Request` compatibility. |
| `frontend/README.md` | Replaced the default Vite template with HostelOS setup, proxy, API, test, and limitation documentation. |
| `docs/CURRENT_STATE.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md` | Updated to reflect the integrated system-info slice. |

No new dependency was added. No backend file was edited. No commit was created
because `D:\Downloads\HostelOs` is not a Git repository.

## Verification results

### Static checks and frontend gates

```text
npm ls --depth=0
  clean; no missing or invalid dependency reported

npm run lint
  completed with no ESLint output or errors

npm test
  Test Files  4 passed (4)
  Tests       13 passed (13)
  Duration    3.92s

npm run build
  2002 modules transformed
  built in 699ms
  dist/assets/SystemPage-CoZpE-fy.js   7.85 kB (gzip 2.47 kB)
  dist/assets/index-D0ErRgZ7.js      404.08 kB (gzip 129.38 kB)
```

The final lint, test, and build run occurred after the last page-style edits.

The 13 tests consist of the four existing foundation tests, four API tests, and
five page tests. New coverage includes the effective request path, successful
data rendering, preserved backend status, HTTP failure, malformed metadata,
loading, safe error text, retry, and navigation from the application shell.

### Real API and browser smoke test

With the backend on port 8080 and Vite on port 5173:

```text
GET http://localhost:8080/api/v1/system/info
  200 {"applicationName":"HostelOS","apiVersion":"v1","status":"AVAILABLE"}

GET http://localhost:5173/api/v1/system/info
  200 {"applicationName":"HostelOS","apiVersion":"v1","status":"AVAILABLE"}

POST http://localhost:8080/api/v1/system/info
  401
```

Headless browser checks:

- `/system` rendered the title `System · HostelOS`, the heading
  `System information`, and the values `HostelOS`, `v1`, and `AVAILABLE`.
- No alert was rendered; console errors and failed network requests were 0.
- `/` rendered `Welcome to HostelOS`.
- `/foundation` rendered `A clear base for every module`.
- `/missing-page` rendered `Page not found`.
- At a 390px viewport, the page had no horizontal overflow and retained all
  metadata values.
- Desktop and mobile screenshots were inspected.

The component tests mock `fetch`. The direct endpoint, Vite proxy, and browser
checks above are separate real-integration evidence.

### Test environment note

Node's `Request` constructor rejects a relative URL, so Vitest uses
`frontend/.env.test` with `VITE_API_BASE_URL=http://localhost/api`. This is
test-only configuration. Development and production continue to use the
relative `/api` base URL and the Vite/deployment proxy.

## Known limitations and follow-ups

1. **Status contract mismatch (historical).** The backend returns `AVAILABLE`;
   the brief expected `UP`. Finalization accepts `AVAILABLE` as the contract.
   The frontend preserves the backend value; `UP` remains only as historical
   context.
2. **Proxy-only development path.** No backend CORS configuration exists.
   Direct browser requests from the Vite origin are not supported; local
   integration relies on the Vite proxy.
3. **No Git repository.** A diff, change-attribution review, tracked-secret
   proof, and commit remain blocked. Git was not initialized.
4. **No CI.** The verified lint, test, and build commands are manual.
5. **No automated browser test.** The browser path was checked manually; the
   test suite covers mocked API behavior rather than a real browser-to-backend
   assertion.
6. **No dedicated empty visual state.** Missing or empty required metadata is
   treated as an invalid response and rendered through the safe error state.
7. **Backend suite not rerun here.** No backend file was changed. The endpoint
   was verified at runtime; the earlier foundation backend result remains 7/7
   tests.
8. **No authentication.** The endpoint is intentionally public for this slice;
   all other unlisted API routes remain denied.

The backend and Vite processes started for this verification were stopped, and
ports 8080 and 5173 were confirmed free.
