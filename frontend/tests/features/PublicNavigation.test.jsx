import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { jsonResponse, stubFetchWithMemberships } from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

function assertLinkHref(name, href) {
  const anchors = Array.from(document.querySelectorAll('a'))
  const match = anchors.find(
    (a) => a.textContent.trim() === name && a.getAttribute('href') === href,
  )
  expect(match, `expected an anchor "${name}" -> ${href}`).toBeInTheDocument()
}

describe('public website navigation', () => {
  it('renders public pages for anonymous users', async () => {
    const cases = [
      { route: '/', heading: /discover hostels and pgs/i },
      { route: '/discover', heading: /find hostels and pgs/i },
      { route: '/for-owners', heading: /run your hostel from one calm workspace/i },
      { route: '/features', heading: /built for discovery and operations/i },
      { route: '/about', heading: /about hostelos/i },
      { route: '/contact', heading: /get in touch/i },
    ]

    for (const { route, heading } of cases) {
      const { unmount } = renderApp({ route, authStatus: 'anonymous', user: null })
      expect(await screen.findByRole('heading', { name: heading })).toBeInTheDocument()
      unmount()
    }
  })

  it('renders a Sign In link pointing to /login', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/', authStatus: 'anonymous', user: null })
    await screen.findByRole('heading', { name: /discover hostels and pgs/i })

    assertLinkHref('Sign in', '/login')
  })

  it('renders a Get Started link pointing to /register', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/', authStatus: 'anonymous', user: null })
    await screen.findByRole('heading', { name: /discover hostels and pgs/i })

    assertLinkHref('Get started', '/register')
  })

  it('renders a brand link to /', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/contact', authStatus: 'anonymous', user: null })
    await screen.findByRole('heading', { name: /get in touch/i })

    const homeLinks = Array.from(document.querySelectorAll('a')).filter(
      (a) => a.getAttribute('href') === '/',
    )
    expect(homeLinks.length).toBeGreaterThan(0)
  })

  it('renders header nav links with correct destinations', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/', authStatus: 'anonymous', user: null })
    await screen.findByRole('heading', { name: /discover hostels and pgs/i })

    assertLinkHref('Discover', '/discover')
    assertLinkHref('For Owners', '/for-owners')
    assertLinkHref('Features', '/features')
    assertLinkHref('About', '/about')
    assertLinkHref('Contact', '/contact')
  })

  it('renders footer links with correct destinations', async () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/', authStatus: 'anonymous', user: null })
    await screen.findByRole('heading', { name: /discover hostels and pgs/i })

    const footer = document.querySelector('footer')
    expect(footer.querySelector('a[href="/discover"]')).toBeInTheDocument()
    expect(footer.querySelector('a[href="/for-owners"]')).toBeInTheDocument()
    expect(footer.querySelector('a[href="/features"]')).toBeInTheDocument()
    expect(footer.querySelector('a[href="/about"]')).toBeInTheDocument()
    expect(footer.querySelector('a[href="/contact"]')).toBeInTheDocument()
    expect(footer.querySelector('a[href="/login"]')).toBeInTheDocument()
    expect(footer.querySelector('a[href="/register"]')).toBeInTheDocument()
  })

  it('has no nested anchor elements', () => {
    stubFetchWithMemberships(
      [],
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    const { container } = renderApp({ route: '/', authStatus: 'anonymous', user: null })
    const anchors = container.querySelectorAll('a')
    anchors.forEach((anchor) => {
      expect(anchor.querySelector('a')).toBeNull()
    })
  })
})