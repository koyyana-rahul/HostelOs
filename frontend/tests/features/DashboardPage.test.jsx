import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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

  it('renders semantic activity entries with contextual labels and timestamps', async () => {
    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    const list = screen.getByRole('list', { name: /recent activity feed/i })
    const items = within(list).getAllByRole('listitem')
    expect(items).toHaveLength(4)

    expect(within(items[0]).getByText('Resident check-in confirmed')).toBeInTheDocument()
    expect(within(items[0]).getByText('Room 204 · Green Valley Hostel')).toBeInTheDocument()
    expect(within(items[0]).getByText('10 minutes ago')).toBeInTheDocument()

    expect(within(items[1]).getByText('Work order dispatched')).toBeInTheDocument()
    expect(within(items[1]).getByText('HVAC filter replacement · Tech Park')).toBeInTheDocument()
    expect(within(items[1]).getByText('35 minutes ago')).toBeInTheDocument()

    expect(within(items[2]).getByText('Resident check-out completed')).toBeInTheDocument()
    expect(within(items[2]).getByText('Room 108 · Downtown Scholars')).toBeInTheDocument()
    expect(within(items[2]).getByText('2 hours ago')).toBeInTheDocument()

    expect(within(items[3]).getByText('Resident check-in confirmed')).toBeInTheDocument()
    expect(within(items[3]).getByText('Room 312 · Green Valley Hostel')).toBeInTheDocument()
    expect(within(items[3]).getByText('4 hours ago')).toBeInTheDocument()
  })

  it('provides a working link to the Foundation page from the dashboard', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/foundation', user: organizationAdminUserPayload })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    expect(screen.getByText(/foundation/i)).toBeInTheDocument()
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
