import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AppProviders } from '../../src/app/providers.jsx'
import { createAppStore } from '../../src/app/store.js'
import { RouteError } from '../../src/components/feedback/RouteError.jsx'
import { baseApi } from '../../src/services/api/baseApi.js'
import { jsonResponse, stubFetch } from '../helpers/api.js'
import { createTestAuthState, renderApp } from '../test-utils.jsx'

// Unique, test-only endpoint definition with non-overlapping name and URL
const testIsolationApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAccessControlProbe: builder.query({
      query: () => '/v1/test-isolation/probe',
    }),
  }),
  overrideExisting: false,
})

const { useGetAccessControlProbeQuery } = testIsolationApi

function TestProbeConsumer() {
  const { data, error, isLoading } = useGetAccessControlProbeQuery()

  if (isLoading) {
    return <div>Loading test probe...</div>
  }

  if (error) {
    return <div>Test probe error: {error.status}</div>
  }

  return <div>Test probe data: {data?.status}</div>
}

describe('Access Control and Route Error Handling', () => {
  let activeStore = null

  afterEach(() => {
    cleanup()
    if (activeStore) {
      activeStore.dispatch(baseApi.util.resetApiState())
      activeStore = null
    }
    vi.restoreAllMocks()
  })

  describe('unauthorized route access', () => {
    it('redirects unauthenticated users from /unauthorized to /login', async () => {
      renderApp({ route: '/unauthorized', authStatus: 'anonymous' })

      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /sign in to hostelos/i,
        }),
      ).toBeInTheDocument()
    })

    it('renders AccessDenied inside AppLayout for authenticated users', async () => {
      renderApp({ route: '/unauthorized', authStatus: 'authenticated' })

      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /^access denied$/i,
        }),
      ).toBeInTheDocument()

      expect(
        screen.getByText(/you do not have permission to access this section/i),
      ).toBeInTheDocument()

      // AppLayout shell elements remain rendered
      expect(screen.getByText('Operations workspace')).toBeInTheDocument()
      const nav = screen.getByRole('navigation', { name: /primary/i })
      expect(nav).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'Overview' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'Foundation' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'System' })).toBeInTheDocument()

      expect(document.title).toBe('Access denied · HostelOS')
    })
  })

  describe('RouteError boundary handling', () => {
    it('renders access-denied view with working overview link on router 403 response', async () => {
      const user = userEvent.setup()
      const testRoutes = [
        {
          path: '/',
          ErrorBoundary: RouteError,
          children: [
            {
              path: 'forbidden-view',
              loader: () => {
                throw new Response('Forbidden', {
                  status: 403,
                  statusText: 'Forbidden',
                })
              },
              Component: () => null,
            },
            {
              index: true,
              Component: () => <h1>Welcome to HostelOS</h1>,
            },
          ],
        },
      ]

      const router = createMemoryRouter(testRoutes, {
        initialEntries: ['/forbidden-view'],
      })

      render(<RouterProvider router={router} />)

      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /^access denied$/i,
        }),
      ).toBeInTheDocument()

      const overviewLink = screen.getByRole('link', {
        name: /back to overview/i,
      })
      expect(overviewLink).toBeInTheDocument()

      await user.click(overviewLink)

      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /welcome to hostelos/i,
        }),
      ).toBeInTheDocument()
    })

    it('renders not-found view with working overview link on router 404 response', async () => {
      const user = userEvent.setup()
      const testRoutes = [
        {
          path: '/',
          ErrorBoundary: RouteError,
          children: [
            {
              path: 'missing-view',
              loader: () => {
                throw new Response('Not Found', {
                  status: 404,
                  statusText: 'Not Found',
                })
              },
              Component: () => null,
            },
            {
              index: true,
              Component: () => <h1>Welcome to HostelOS</h1>,
            },
          ],
        },
      ]

      const router = createMemoryRouter(testRoutes, {
        initialEntries: ['/missing-view'],
      })

      render(<RouterProvider router={router} />)

      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /^page not found$/i,
        }),
      ).toBeInTheDocument()

      const overviewLink = screen.getByRole('link', {
        name: /back to overview/i,
      })
      expect(overviewLink).toBeInTheDocument()

      await user.click(overviewLink)

      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /welcome to hostelos/i,
        }),
      ).toBeInTheDocument()
    })

    it('retains reload fallback view on unexpected runtime exceptions', async () => {
      const testRoutes = [
        {
          path: '/',
          ErrorBoundary: RouteError,
          children: [
            {
              path: 'crashed-view',
              loader: () => {
                throw new Error('Unexpected runtime exception')
              },
              Component: () => null,
            },
          ],
        },
      ]

      const router = createMemoryRouter(testRoutes, {
        initialEntries: ['/crashed-view'],
      })

      render(<RouterProvider router={router} />)

      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /this view could not be rendered/i,
        }),
      ).toBeInTheDocument()

      expect(
        screen.getByRole('button', { name: /reload page/i }),
      ).toBeInTheDocument()
    })
  })

  describe('RTK Query 401 vs 403 authentication state settling', () => {
    it('settles with active authentication on HTTP 403 without clearing user session', async () => {
      stubFetch(jsonResponse({ message: 'Access forbidden by backend' }, 403))

      activeStore = createAppStore({
        auth: createTestAuthState('authenticated'),
      })

      render(
        <AppProviders store={activeStore}>
          <TestProbeConsumer />
        </AppProviders>,
      )

      expect(
        await screen.findByText(/test probe error: 403/i),
      ).toBeInTheDocument()

      const settledAuth = activeStore.getState().auth
      expect(settledAuth.status).toBe('authenticated')
      expect(settledAuth.user).not.toBeNull()
      expect(settledAuth.user.displayName).toBe('Platform Administrator')
      expect(settledAuth.user.roles).toContain('PLATFORM_ADMIN')
    })

    it('clears session and transitions to anonymous on HTTP 401', async () => {
      stubFetch(jsonResponse({ message: 'Authentication required' }, 401))

      activeStore = createAppStore({
        auth: createTestAuthState('authenticated'),
      })

      render(
        <AppProviders store={activeStore}>
          <TestProbeConsumer />
        </AppProviders>,
      )

      expect(
        await screen.findByText(/test probe error: 401/i),
      ).toBeInTheDocument()

      const settledAuth = activeStore.getState().auth
      expect(settledAuth.status).toBe('anonymous')
      expect(settledAuth.user).toBeNull()
    })
  })
})
