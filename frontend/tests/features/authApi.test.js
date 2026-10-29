import { describe, expect, it } from 'vitest'
import { createAppStore } from '../../src/app/store.js'
import { authApi } from '../../src/features/auth/api/authApi.js'
import {
  authUserPayload,
  csrfPayload,
  jsonResponse,
  noContentResponse,
  stubFetch,
} from '../helpers/api.js'
import { createTestAuthState } from '../test-utils.jsx'

describe('authentication API', () => {
  it('loads the current identity with cookies included', async () => {
    const fetchMock = stubFetch(jsonResponse(authUserPayload))
    const store = createAppStore({
      auth: createTestAuthState('checking'),
    })

    const result = await store.dispatch(
      authApi.endpoints.getCurrentUser.initiate(),
    )

    const request = fetchMock.mock.calls[0][0]

    expect(new URL(request.url).pathname).toBe('/api/v1/auth/me')
    expect(request.method).toBe('GET')
    expect(request.credentials).toBe('include')
    expect(result.data).toEqual(authUserPayload)
  })

  it('bootstraps a CSRF token for session mutations', async () => {
    const fetchMock = stubFetch(jsonResponse(csrfPayload))
    const store = createAppStore()

    const result = await store.dispatch(
      authApi.endpoints.getCsrfToken.initiate(),
    )

    const request = fetchMock.mock.calls[0][0]

    expect(new URL(request.url).pathname).toBe('/api/v1/auth/csrf')
    expect(request.method).toBe('GET')
    expect(request.credentials).toBe('include')
    expect(result.data).toEqual(csrfPayload)
  })

  it('posts credentials with the server-provided CSRF header', async () => {
    const fetchMock = stubFetch(jsonResponse(authUserPayload))
    const store = createAppStore()
    const credentials = {
      loginIdentifier: 'platform-admin@example.com',
      password: 'correct-horse-battery-staple',
      csrf: csrfPayload,
    }

    const result = await store.dispatch(
      authApi.endpoints.login.initiate(credentials),
    )

    const request = fetchMock.mock.calls[0][0]

    expect(new URL(request.url).pathname).toBe('/api/v1/auth/login')
    expect(request.method).toBe('POST')
    expect(request.credentials).toBe('include')
    expect(request.headers.get('X-XSRF-TOKEN')).toBe(csrfPayload.token)
    expect(request.headers.get('content-type')).toContain('application/json')
    await expect(request.json()).resolves.toEqual({
      loginIdentifier: credentials.loginIdentifier,
      password: credentials.password,
    })
    expect(result.data).toEqual(authUserPayload)
  })

  it('logs out with CSRF protection and no response body', async () => {
    const fetchMock = stubFetch(noContentResponse())
    const store = createAppStore()

    const result = await store.dispatch(
      authApi.endpoints.logout.initiate(csrfPayload),
    )

    const request = fetchMock.mock.calls[0][0]

    expect(new URL(request.url).pathname).toBe('/api/v1/auth/logout')
    expect(request.method).toBe('POST')
    expect(request.credentials).toBe('include')
    expect(request.headers.get('X-XSRF-TOKEN')).toBe(csrfPayload.token)
    expect(result.data).toBeNull()
  })

  it('preserves the finalized platform, organization, and staff role names', async () => {
    const identity = {
      ...authUserPayload,
      roles: ['PLATFORM_ADMIN', 'ORGANIZATION_ADMIN', 'STAFF'],
    }
    stubFetch(jsonResponse(identity))
    const store = createAppStore({
      auth: createTestAuthState('checking'),
    })

    const result = await store.dispatch(
      authApi.endpoints.getCurrentUser.initiate(),
    )

    expect(result.data.roles).toEqual([
      'PLATFORM_ADMIN',
      'ORGANIZATION_ADMIN',
      'STAFF',
    ])
  })

  it('clears a local session when the current-user request returns 401', async () => {
    stubFetch(
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )
    const store = createAppStore({
      auth: createTestAuthState('authenticated'),
    })

    const result = await store.dispatch(
      authApi.endpoints.getCurrentUser.initiate(undefined, {
        forceRefetch: true,
      }),
    )

    expect(result.error.status).toBe(401)
    expect(store.getState().auth.status).toBe('anonymous')
    expect(store.getState().auth.user).toBeNull()
  })
})
