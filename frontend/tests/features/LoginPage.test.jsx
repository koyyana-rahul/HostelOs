import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import {
  authUserPayload,
  csrfPayload,
  jsonResponse,
  networkError,
  stubFetch,
} from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

async function completeSignIn(
  user,
  identifier = 'platform-admin@example.com',
) {
  await user.type(
    await screen.findByLabelText(/email or username/i),
    identifier,
  )
  await user.type(
    screen.getByLabelText(/^password$/i),
    'correct-horse-battery-staple',
  )
  await user.click(screen.getByRole('button', { name: /^sign in$/i }))
}

describe('sign-in page', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('renders an accessible sign-in form without the application shell', async () => {
    renderApp({ route: '/login', authStatus: 'anonymous' })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /sign in to hostelos/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText(/email or username/i)).toHaveAttribute(
      'autocomplete',
      'username',
    )
    expect(screen.getByLabelText(/^password$/i)).toHaveAttribute(
      'autocomplete',
      'current-password',
    )
    expect(
      screen.getByRole('button', { name: /^sign in$/i }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('navigation', { name: /primary/i }),
    ).not.toBeInTheDocument()
    expect(document.title).toBe('Sign in · HostelOS')
  })

  it('creates a session and returns to the requested protected page', async () => {
    const fetchMock = stubFetch(
      jsonResponse(csrfPayload),
      jsonResponse(authUserPayload),
    )
    const user = userEvent.setup()

    renderApp({ route: '/foundation', authStatus: 'anonymous' })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /sign in to hostelos/i,
      }),
    ).toBeInTheDocument()

    await completeSignIn(user)

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /a clear base for every module/i,
      }),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)

    const csrfRequest = fetchMock.mock.calls[0][0]
    const loginRequest = fetchMock.mock.calls[1][0]

    expect(new URL(csrfRequest.url).pathname).toBe('/api/v1/auth/csrf')
    expect(new URL(loginRequest.url).pathname).toBe('/api/v1/auth/login')
    expect(loginRequest.headers.get('X-XSRF-TOKEN')).toBe(csrfPayload.token)
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('shows a safe message for invalid credentials', async () => {
    stubFetch(
      jsonResponse(csrfPayload),
      jsonResponse({ status: 401, message: 'Invalid credentials' }, 401),
    )
    const user = userEvent.setup()

    renderApp({ route: '/login', authStatus: 'anonymous' })

    await completeSignIn(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /login identifier or password is incorrect/i,
    )
    expect(
      screen.getByRole('heading', { level: 1, name: /sign in to hostelos/i }),
    ).toBeInTheDocument()
  })

  it('keeps disabled-account failures indistinguishable from invalid credentials', async () => {
    stubFetch(
      jsonResponse(csrfPayload),
      jsonResponse({ status: 401, message: 'Account is disabled' }, 401),
    )
    const user = userEvent.setup()

    renderApp({ route: '/login', authStatus: 'anonymous' })

    await completeSignIn(user)

    const alert = await screen.findByRole('alert')

    expect(alert).toHaveTextContent(/login identifier or password is incorrect/i)
    expect(alert).not.toHaveTextContent(/disabled/i)
  })

  it('asks the user to retry when CSRF validation is rejected', async () => {
    stubFetch(
      jsonResponse(csrfPayload),
      jsonResponse({ status: 403, message: 'Forbidden' }, 403),
    )
    const user = userEvent.setup()

    renderApp({ route: '/login', authStatus: 'anonymous' })

    await completeSignIn(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /security check could not be completed/i,
    )
  })

  it('explains login throttling without exposing backend details', async () => {
    stubFetch(
      jsonResponse(csrfPayload),
      jsonResponse(
        { status: 429, message: 'Too many authentication attempts' },
        429,
      ),
    )
    const user = userEvent.setup()

    renderApp({ route: '/login', authStatus: 'anonymous' })

    await completeSignIn(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /too many sign-in attempts/i,
    )
    expect(screen.getByRole('alert')).not.toHaveTextContent(
      /too many authentication attempts/i,
    )
  })

  it('shows a safe error when the authentication service cannot be reached', async () => {
    stubFetch(networkError())
    const user = userEvent.setup()

    renderApp({ route: '/login', authStatus: 'anonymous' })

    await completeSignIn(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /authentication service could not be reached/i,
    )
  })

  it('validates empty credentials before contacting the API', async () => {
    const user = userEvent.setup()

    renderApp({ route: '/login', authStatus: 'anonymous' })

    await user.click(
      await screen.findByRole('button', { name: /^sign in$/i }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /enter both your login identifier and password/i,
    )
  })

  it('shows progress while the sign-in request is pending', async () => {
    stubFetch(jsonResponse(csrfPayload), new Promise(() => {}))
    const user = userEvent.setup()

    renderApp({ route: '/login', authStatus: 'anonymous' })

    await completeSignIn(user)

    expect(
      await screen.findByRole('button', { name: /signing in/i }),
    ).toBeDisabled()
    expect(screen.getByLabelText(/email or username/i)).toBeDisabled()
  })

  it('redirects an existing session away from the sign-in page', async () => {
    renderApp({ route: '/login' })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /welcome to hostelos/i,
      }),
    ).toBeInTheDocument()
  })
})
