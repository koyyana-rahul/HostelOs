import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  authUserPayload,
  organizationAdminUserPayload,
  staffUserPayload,
  stubFetch,
  stubFetchWithMemberships,
} from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

describe('role-aware route access', () => {
  it('PLATFORM_ADMIN can open /platform', async () => {
    stubFetch()

    const { router } = renderApp({ route: '/platform', user: authUserPayload })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /platform dashboard/i,
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/platform')
  })

  it('CUSTOMER without active organization membership is sent to /onboarding', async () => {
    const customerUser = {
      id: 99,
      loginIdentifier: 'customer@example.com',
      displayName: 'Customer',
      roles: [],
    }

    stubFetch()

    const { router } = renderApp({ route: '/app', user: customerUser })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /get started with hostelos/i,
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/onboarding')
  })

  it('ORGANIZATION_ADMIN with active membership can open /app', async () => {
    const memberships = [
      {
        id: 1,
        organizationId: 10,
        organizationName: 'Green Valley Hostel',
        orgRole: 'ORG_ADMIN',
        status: 'ACTIVE',
      },
    ]

    stubFetchWithMemberships(memberships)

    const { router } = renderApp({
      route: '/app',
      user: organizationAdminUserPayload,
    })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /^overview$/i,
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app')
  })

  it('STAFF with active membership can open /app', async () => {
    const memberships = [
      {
        id: 2,
        organizationId: 10,
        organizationName: 'Green Valley Hostel',
        orgRole: 'ORG_STAFF',
        status: 'ACTIVE',
      },
    ]

    stubFetchWithMemberships(memberships)

    const { router } = renderApp({
      route: '/app',
      user: staffUserPayload,
    })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /^overview$/i,
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app')
  })

  it('Anonymous user is sent to /login from a protected route', async () => {
    const { router } = renderApp({
      route: '/foundation',
      authStatus: 'anonymous',
      user: null,
    })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /sign in to hostelos/i,
      }),
    ).toBeInTheDocument()
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/login')
    })
  })
})
