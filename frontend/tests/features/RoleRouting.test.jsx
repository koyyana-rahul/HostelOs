import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  authUserPayload,
  jsonResponse,
  organizationAdminUserPayload,
  staffUserPayload,
  stubFetch,
} from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

describe('role-based routing', () => {
  it('PLATFORM_ADMIN redirects to /platform', async () => {
    renderApp({ route: '/', user: authUserPayload })

    await waitFor(() => {
      expect(window.location.pathname).toBe('/platform')
    })
    expect(await screen.findByText(/platform/i)).toBeInTheDocument()
  })

  it('CUSTOMER without active organization membership redirects to /onboarding', async () => {
    const customerUser = {
      id: 99,
      loginIdentifier: 'customer@example.com',
      displayName: 'Customer',
      roles: [],
    }
    stubFetch(jsonResponse([]))

    renderApp({ route: '/', user: customerUser })

    await waitFor(() => {
      expect(window.location.pathname).toBe('/onboarding')
    })
  })

  it('ORGANIZATION_ADMIN with active membership redirects to /app', async () => {
    const memberships = [
      { id: 1, organizationId: 10, orgRole: 'ORG_ADMIN', status: 'ACTIVE' },
    ]
    stubFetch(jsonResponse(memberships))

    renderApp({ route: '/', user: organizationAdminUserPayload })

    await waitFor(() => {
      expect(window.location.pathname).toBe('/app')
    })
  })

  it('STAFF with active membership redirects to /app', async () => {
    const memberships = [
      { id: 2, organizationId: 10, orgRole: 'ORG_STAFF', status: 'ACTIVE' },
    ]
    stubFetch(jsonResponse(memberships))

    renderApp({ route: '/', user: staffUserPayload })

    await waitFor(() => {
      expect(window.location.pathname).toBe('/app')
    })
  })

  it('Anonymous user redirects to /login', async () => {
    renderApp({ route: '/', authStatus: 'anonymous', user: null })

    await waitFor(() => {
      expect(window.location.pathname).toBe('/login')
    })
  })
})
