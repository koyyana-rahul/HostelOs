import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { stubFetch } from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

describe('application shell', () => {
  it('renders the application shell at the foundation route', async () => {
    stubFetch()
    renderApp({ route: '/foundation' })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /a clear base for every module/i,
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('navigation', { name: /primary/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Foundation' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(screen.getByText('Platform Administrator')).toBeInTheDocument()
    expect(screen.getByText('platform-admin@example.com')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeInTheDocument()
    await waitFor(() => {
      expect(document.title).toBe('Foundation · HostelOS')
    })
  })

  it('opens and closes the mobile navigation', async () => {
    const user = userEvent.setup()
    stubFetch()
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const openButton = screen.getByRole('button', {
      name: /open navigation/i,
    })
    await user.click(openButton)

    expect(
      screen.getByRole('dialog', { name: /application navigation/i }),
    ).toBeInTheDocument()
    expect(openButton).toHaveAttribute('aria-expanded', 'true')

    await user.click(
      screen.getByRole('button', { name: /close navigation/i }),
    )

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(openButton).toHaveAttribute('aria-expanded', 'false')
  })
})
