import { act, fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  authUserPayload,
  csrfPayload,
  jsonResponse,
  networkError,
  noContentResponse,
  stubFetch,
} from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

describe('authentication routing', () => {
  it('restores an existing server session before rendering the shell', async () => {
    let resolveSession
    const pendingSession = new Promise((resolve) => {
      resolveSession = resolve
    })
    const fetchMock = stubFetch(pendingSession)

    renderApp({ route: '/', authStatus: 'checking' })

    expect(
      await screen.findByText(/checking your session/i),
    ).toBeInTheDocument()

    await act(async () => {
      resolveSession(jsonResponse(authUserPayload))
    })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /welcome to hostelos/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('Platform Administrator')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(
      new URL(fetchMock.mock.calls[0][0].url).pathname,
    ).toBe('/api/v1/auth/me')
  })

  it('redirects to sign-in when no session exists', async () => {
    stubFetch(
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp({ route: '/', authStatus: 'checking' })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /sign in to hostelos/i,
      }),
    ).toBeInTheDocument()
  })

  it('shows a retryable state when the session check cannot be reached', async () => {
    const fetchMock = stubFetch(
      networkError(),
      jsonResponse(authUserPayload),
    )
    const user = userEvent.setup()

    renderApp({ route: '/', authStatus: 'checking' })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /authentication unavailable/i,
      }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /try again/i }))

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /welcome to hostelos/i,
      }),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('signs out through the backend and clears the shell', async () => {
    const fetchMock = stubFetch(
      jsonResponse(csrfPayload),
      noContentResponse(),
    )
    const user = userEvent.setup()

    renderApp()

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })
    await user.click(screen.getByRole('button', { name: /^sign out$/i }))

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /sign in to hostelos/i,
      }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('navigation', { name: /primary/i }),
    ).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)

    const csrfRequest = fetchMock.mock.calls[0][0]
    const logoutRequest = fetchMock.mock.calls[1][0]

    expect(new URL(csrfRequest.url).pathname).toBe('/api/v1/auth/csrf')
    expect(new URL(logoutRequest.url).pathname).toBe('/api/v1/auth/logout')
    expect(logoutRequest.headers.get('X-XSRF-TOKEN')).toBe(csrfPayload.token)
  })

  it('returns to sign-in when a background session check gets 401', async () => {
    stubFetch(
      jsonResponse({ status: 401, message: 'Authentication is required' }, 401),
    )

    renderApp()

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    fireEvent.focus(window)

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /sign in to hostelos/i,
      }),
    ).toBeInTheDocument()
  })
})
