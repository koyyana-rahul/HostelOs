import { vi } from 'vitest'

export const systemInfoPayload = Object.freeze({
  applicationName: 'HostelOS',
  apiVersion: 'v1',
  status: 'AVAILABLE',
})

export const authUserPayload = Object.freeze({
  id: 1,
  loginIdentifier: 'platform-admin@example.com',
  displayName: 'Platform Administrator',
  roles: ['PLATFORM_ADMIN'],
})

export const organizationAdminUserPayload = Object.freeze({
  id: 2,
  loginIdentifier: 'organization-admin@example.com',
  displayName: 'Organization Administrator',
  roles: ['ORGANIZATION_ADMIN'],
})

export const staffUserPayload = Object.freeze({
  id: 3,
  loginIdentifier: 'staff@example.com',
  displayName: 'HostelOS Staff',
  roles: ['STAFF'],
})

export const csrfPayload = Object.freeze({
  headerName: 'X-XSRF-TOKEN',
  token: 'test-csrf-token',
})

export function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function noContentResponse() {
  return new Response(null, { status: 204 })
}

export function networkError() {
  return () => Promise.reject(new TypeError('Failed to fetch'))
}

export function stubFetch(...responses) {
  const fetchMock = vi.fn()

  responses.forEach((response) => {
    if (typeof response === 'function') {
      fetchMock.mockImplementationOnce(response)
    } else if (response instanceof Promise) {
      fetchMock.mockReturnValueOnce(response)
    } else {
      fetchMock.mockResolvedValueOnce(response)
    }
  })

  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
