import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Brand } from '../../src/components/layout/Brand.jsx'
import { renderApp } from '../test-utils.jsx'
import { jsonResponse, stubFetchWithMemberships } from '../helpers/api.js'
import { organizationAdminUserPayload } from '../helpers/api.js'

describe('Brand component', () => {
  it('renders a single anchor to the home page', () => {
    render(
      <MemoryRouter>
        <Brand />
      </MemoryRouter>,
    )
    const links = document.querySelectorAll('a')
    expect(links).toHaveLength(1)
    expect(links[0]).toHaveAttribute('href', '/')
  })

  it('does not nest an anchor inside another anchor', () => {
    render(
      <MemoryRouter>
        <Brand />
      </MemoryRouter>,
    )
    const anchors = document.querySelectorAll('a')
    anchors.forEach((anchor) => {
      expect(anchor.querySelector('a')).toBeNull()
    })
  })

  it('renders the app name and operations label', () => {
    render(
      <MemoryRouter>
        <Brand />
      </MemoryRouter>,
    )
    expect(document.querySelector('a')?.textContent).toContain('HostelOS')
    expect(document.querySelector('a')?.textContent).toContain('Operations')
  })
})

describe('PublicHeader component', () => {
  it('renders without nested anchors for anonymous users', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/', authStatus: 'anonymous', user: null })

    await screen.findByText(/find a hostel or pg/i)
    const anchors = document.querySelectorAll('a')
    anchors.forEach((anchor) => {
      expect(anchor.querySelector('a')).toBeNull()
    })
  })

  it('renders a home link from the brand for anonymous users', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/', authStatus: 'anonymous', user: null })

    await screen.findByText(/find a hostel or pg/i)
    const homeLinks = Array.from(document.querySelectorAll('a')).filter(
      (a) => a.getAttribute('href') === '/',
    )
    expect(homeLinks.length).toBeGreaterThanOrEqual(1)
  })

  it('renders sign in and get started links for anonymous users', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/', authStatus: 'anonymous', user: null })

    await screen.findByText(/find a hostel or pg/i)
    expect(document.querySelector('a[href="/login"]')).toBeInTheDocument()
    expect(document.querySelector('a[href="/register"]')).toBeInTheDocument()
  })

  it('renders owner dashboard navigation for an ORGANIZATION_ADMIN user', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({
      route: '/',
      authStatus: 'authenticated',
      user: { ...organizationAdminUserPayload, roles: ['ORGANIZATION_ADMIN'] },
    })

    expect(await screen.findByRole('link', { name: /owner dashboard/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Properties' })).toBeInTheDocument()
    const discoverLinks = screen.getAllByRole('link', { name: 'Discover' })
    expect(discoverLinks.length).toBeGreaterThanOrEqual(1)
  })
})