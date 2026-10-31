import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  authUserPayload,
  organizationAdminUserPayload,
  staffUserPayload,
  stubFetch,
  stubOwnerApi,
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

  it('CUSTOMER without an owner account is sent to /discover', async () => {
    const customerUser = {
      id: 99,
      loginIdentifier: 'customer@example.com',
      displayName: 'Customer',
      roles: ['CUSTOMER'],
    }

    stubFetch()

    const { router } = renderApp({ route: '/app', user: customerUser })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /find hostels and pgs/i,
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/discover')
  })

  it('ORGANIZATION_ADMIN with an organization can open /app', async () => {
    stubOwnerApi()

    const { router } = renderApp({
      route: '/app',
      user: { ...organizationAdminUserPayload, roles: ['ORGANIZATION_ADMIN'] },
    })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /^overview$/i,
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app')
  })

  it('STAFF without an owner organization is sent to /discover', async () => {
    stubFetch()

    const { router } = renderApp({
      route: '/app',
      user: { ...staffUserPayload, roles: ['STAFF'] },
    })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /find hostels and pgs/i,
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/discover')
  })
})