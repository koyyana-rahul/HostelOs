import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test-utils.jsx'

describe('application routing', () => {
  it('navigates to the foundation page', async () => {
    const user = userEvent.setup()
    renderApp()

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    await user.click(screen.getByRole('link', { name: 'Foundation' }))

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /a clear base for every module/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Foundation' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(document.title).toBe('Foundation · HostelOS')
  })

  it('renders the not-found state for unknown routes', async () => {
    renderApp({ route: '/missing-page' })

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /page not found/i,
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /back to overview/i }),
    ).toHaveAttribute('href', '/')
  })
})
