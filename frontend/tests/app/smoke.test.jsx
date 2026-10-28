import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test-utils.jsx'

describe('application shell', () => {
  it('renders the HostelOS overview at the root route', async () => {
    renderApp()

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /welcome to hostelos/i,
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('navigation', { name: /primary/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Overview' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(screen.getByText('Frontend ready')).toBeInTheDocument()
    expect(document.title).toBe('Overview · HostelOS')
  })

  it('opens and closes the mobile navigation', async () => {
    const user = userEvent.setup()
    renderApp()

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
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
