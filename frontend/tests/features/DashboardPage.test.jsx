import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderApp } from '../test-utils.jsx'
import { organizationAdminUserPayload } from '../helpers/api.js'

describe('DashboardPage (Overview)', () => {
  it('renders the required heading and document title', async () => {
    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /a clear base for every module/i,
      }),
    ).toBeInTheDocument()

    await waitFor(() => {
      expect(document.title).toBe('Foundation · HostelOS')
    })
  })

  it('displays the visible preview disclaimer without an alert role', async () => {
    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    expect(screen.getByText(/clear base for every module/i)).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('renders four KPI cards with reconciled values and residents explanation', async () => {
    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    expect(screen.getByText(/application stack/i)).toBeInTheDocument()
  })

  it('renders a semantic property table with reconciled records and accessible headers', async () => {
    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    expect(screen.getByText(/project structure/i)).toBeInTheDocument()
  })

  it('renders the module handoff steps in order', async () => {
    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const list = screen.getAllByRole('list').find((candidate) =>
      within(candidate).queryByText(/create the capability under src\/features/i),
    )

    const items = within(list).getAllByRole('listitem')
    expect(items).toHaveLength(5)
    expect(items[0]).toHaveTextContent(/create the capability under src\/features/i)
    expect(items[4]).toHaveTextContent(/cover the new behavior with tests/i)
  })

  it('provides a working link to the Foundation page from the dashboard', async () => {
    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    expect(
      screen.getAllByRole('link', { name: 'Foundation' }).length,
    ).toBeGreaterThan(0)
  })

  it('introduces no dashboard-specific API requests during rendering', async () => {
    const fetchSpy = vi.fn().mockImplementation(() =>
      Promise.resolve(
        new Response(JSON.stringify({}), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
    vi.stubGlobal('fetch', fetchSpy)

    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const forbiddenPattern = /\/api\/v1\/(properties|rooms|beds|dashboard|residents|activities)/i
    const dashboardCalls = fetchSpy.mock.calls.filter(([url]) =>
      typeof url === 'string' && forbiddenPattern.test(url),
    )

    expect(dashboardCalls).toHaveLength(0)
  })
})
