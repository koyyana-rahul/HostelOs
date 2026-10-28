# HostelOS Backend API

## Module 0.1.2A — System Information Contract

### Development access

| Path | URL |
|---|---|
| Backend direct | `http://localhost:8080/api/v1/system/info` |
| Vite development proxy | `http://localhost:5173/api/v1/system/info` |

The frontend Vite server proxies `/api` to `http://localhost:8080` by default. The target can be overridden with `VITE_API_PROXY_TARGET` in the frontend environment.

## `GET /api/v1/system/info`

Returns stable, non-sensitive application metadata. The endpoint does not query the database.

- Authentication: none
- HTTP status: `200 OK`
- Response content type: `application/json`

### Response body

```json
{
  "applicationName": "HostelOS",
  "apiVersion": "v1",
  "status": "AVAILABLE"
}
```

### Fields

| Field | Type | Value | Meaning |
|---|---|---|---|
| `applicationName` | string | `HostelOS` | Application name |
| `apiVersion` | string | `v1` | Public API contract version |
| `status` | string | `AVAILABLE` | This system-information endpoint is available |

`status` describes the availability of this endpoint only. It is not a claim that MySQL, Flyway, or every application dependency is healthy. Dependency health is reported separately by `/actuator/health`.

### Security

- Within the application API, only `GET /api/v1/system/info` is public.
- The security chain remains deny-by-default; infrastructure endpoints such as health, OpenAPI, Swagger UI, and `/error` remain public.
- `POST`, `PUT`, `DELETE`, and `PATCH` on this path return `401` with a JSON error body.
- `HEAD` on this path returns `401` with no response body, as required by the HTTP method.
- All unlisted application routes return `401` with a JSON error body for requests that reach the security chain.
- No broad `/api/**` wildcard permit was added.
- CSRF remains disabled because this is a stateless API.
- No authentication provider is invented in this module.

Example denied response:

```json
{
  "timestamp": "2026-10-28T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required",
  "path": "/api/v1/system/info",
  "violations": []
}
```

### CORS decision

Development integration is same-origin through the Vite `/api` proxy. No CORS configuration, wildcard origin, or `@CrossOrigin` exception was added.

A separately hosted frontend and backend would require an explicit production-origin decision and strict origin allowlisting. That decision is intentionally deferred.

### OpenAPI

The endpoint is documented in the generated OpenAPI document:

- `/v3/api-docs`
- `/swagger-ui.html`

OpenAPI and Swagger UI are enabled in the default development profile and disabled by the `prod` profile.

### Related health endpoint

`GET /actuator/health` reports application and dependency health. The system-information endpoint deliberately does not duplicate that responsibility.
