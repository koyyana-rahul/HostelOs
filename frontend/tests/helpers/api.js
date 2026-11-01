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

export const organizationPayload = Object.freeze({
  id: 10,
  name: 'Sunrise Stays',
  slug: 'sunrise-stays',
  status: 'ACTIVE',
})

export const dashboardPayload = Object.freeze({
  organizationId: 10,
  propertyCount: 2,
  activePropertyCount: 2,
  listingCount: 1,
  publishedListingCount: 1,
  draftListingCount: 0,
  archivedListingCount: 0,
  enquiryCount: 3,
  newEnquiryCount: 1,
  readEnquiryCount: 0,
  respondedEnquiryCount: 1,
  closedEnquiryCount: 1,
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

const MEMBERSHIPS_PATH = '/v1/auth/me/memberships'

function isMembershipsRequest(input) {
  const url = typeof input === 'string' ? input : input?.url
  return typeof url === 'string' && url.includes(MEMBERSHIPS_PATH)
}

function createFetchStub(memberships, responses) {
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

  fetchMock.mockImplementation(() =>
    Promise.resolve(jsonResponse({ status: 404, message: 'Not found' }, 404)),
  )

  const fetchImplementation = (input, init) => {
    if (isMembershipsRequest(input)) {
      return Promise.resolve(jsonResponse(memberships))
    }

    return fetchMock(input, init)
  }

  vi.stubGlobal('fetch', fetchImplementation)
  return fetchMock
}

export function stubFetch(...responses) {
  return createFetchStub([], responses)
}

export function stubFetchWithMemberships(memberships, ...responses) {
  return createFetchStub(memberships, responses)
}

export function stubOwnerApi({
  organizations = [organizationPayload],
  dashboard = dashboardPayload,
  properties = [],
  listings = [],
  enquiries = [],
  memberships = [],
} = {}) {
  const fetchMock = vi.fn((input) => {
    const url = typeof input === 'string' ? input : input?.url
    const path = typeof url === 'string' ? url : ''

    if (path.includes('/v1/organizations/mine')) {
      return Promise.resolve(jsonResponse(organizations))
    }
    if (path.includes('/dashboard')) {
      return Promise.resolve(jsonResponse(dashboard))
    }
    if (/\/v1\/organizations\/\d+\/properties/.test(path)) {
      return Promise.resolve(jsonResponse(properties))
    }
    if (/\/v1\/organizations\/\d+\/listings/.test(path)) {
      return Promise.resolve(jsonResponse(listings))
    }
    if (/\/v1\/organizations\/\d+\/enquiries/.test(path)) {
      return Promise.resolve(jsonResponse(enquiries))
    }
    if (path.includes('/v1/auth/me/memberships')) {
      return Promise.resolve(jsonResponse(memberships))
    }

    return Promise.resolve(
      jsonResponse({ status: 404, message: 'Not found' }, 404),
    )
  })

  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
