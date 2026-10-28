import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  jsonResponse,
  stubFetch,
  systemInfoPayload,
} from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

describe('system information page', () => {
  it('shows a loading state while the request is pending', async () => {
    stubFetch(new Promise(() => {}))

    renderApp({ route: '/system' })

    expect(
      await screen.findByText(/contacting the backend/i),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/requesting \/api\/v1\/system\/info/i),
    ).toBeInTheDocument()
  })

  it('renders the returned application metadata', async () => {
    stubFetch(jsonResponse(systemInfoPayload))

    renderApp({ route: '/system' })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /system information/i,
      }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('HostelOS', { selector: 'dd' }),
    ).toBeInTheDocument()
    expect(screen.getByText('v1', { selector: 'dd' })).toBeInTheDocument()
    expect(screen.getByText('AVAILABLE')).toBeInTheDocument()
    expect(
      screen.getByText(/does not confirm platform-wide health/i),
    ).toBeInTheDocument()
  })

  it('shows a safe error and retries the request successfully', async () => {
    const fetchMock = stubFetch(
      jsonResponse({ message: 'internal database detail' }, 503),
      jsonResponse(systemInfoPayload),
    )
    const user = userEvent.setup()

    renderApp({ route: '/system' })

    const alert = await screen.findByRole('alert')

    expect(alert).toHaveTextContent(/system information unavailable/i)
    expect(alert).not.toHaveTextContent(/internal database detail/i)

    await user.click(
      screen.getByRole('button', { name: /retry request/i }),
    )

    expect(
      await screen.findByText('HostelOS', { selector: 'dd' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('treats an incomplete response as an error', async () => {
    stubFetch(
      jsonResponse({ applicationName: 'HostelOS', apiVersion: 'v1' }),
    )

    renderApp({ route: '/system' })

    expect(await screen.findByRole('alert')).toHaveTextContent(/incomplete/i)
    expect(
      screen.queryByText('HostelOS', { selector: 'dd' }),
    ).not.toBeInTheDocument()
  })

  it('navigates from the application shell to the system page', async () => {
    stubFetch(jsonResponse(systemInfoPayload))
    const user = userEvent.setup()

    renderApp()

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    await user.click(screen.getByRole('link', { name: 'System' }))

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /system information/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'System' })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })
})
