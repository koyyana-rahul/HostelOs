import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  authUserPayload,
  jsonResponse,
  stubFetch,
  stubOwnerApi,
} from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

const customerUser = {
  id: 5,
  loginIdentifier: 'customer@example.com',
  displayName: 'Customer',
  roles: ['CUSTOMER'],
}

describe('RequirePlatformMembership guard', () => {
  it('redirects to /login when no user is authenticated', async () => {
    stubFetch(jsonResponse({ status: 401, message: 'Authentication is required' }, 401))

    renderApp({ route: '/onboarding', authStatus: 'anonymous', user: null })

    expect(
      await screen.findByRole('heading', { name: /sign in to hostelos/i }),
    ).toBeInTheDocument()
  })

  it('redirects platform admins to /platform', async () => {
    stubFetch()

    renderApp({
      route: '/onboarding',
      authStatus: 'authenticated',
      user: { ...authUserPayload, roles: ['PLATFORM_ADMIN'] },
    })

    expect(
      await screen.findByRole('heading', { name: /platform dashboard/i }),
    ).toBeInTheDocument()
  })

  it('redirects customers without an owner account to /discover', async () => {
    stubFetch()

    renderApp({
      route: '/onboarding',
      authStatus: 'authenticated',
      user: customerUser,
    })

    expect(
      await screen.findByRole('heading', { name: /find hostels and pgs/i }),
    ).toBeInTheDocument()
  })

  it('renders onboarding when the owner has no organization', async () => {
    stubOwnerApi({ organizations: [] })

    renderApp({
      route: '/onboarding',
      authStatus: 'authenticated',
      user: { ...authUserPayload, roles: ['ORGANIZATION_ADMIN'] },
    })

    expect(
      await screen.findByRole('heading', { name: /get started with hostelos/i }),
    ).toBeInTheDocument()
  })

  it('redirects owners with an organization to the owner overview', async () => {
    stubOwnerApi()

    renderApp({
      route: '/onboarding',
      authStatus: 'authenticated',
      user: { ...authUserPayload, roles: ['ORGANIZATION_ADMIN'] },
    })

    expect(
      await screen.findByRole('heading', { name: /^overview$/i }),
    ).toBeInTheDocument()
  })
})