import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import { AccessDenied } from '../../src/components/feedback/AccessDenied.jsx'

describe('AccessDenied component', () => {
  afterEach(() => {
    cleanup()
  })

  it('renders semantic h1 heading, descriptive guidance, and no blanket alert role', () => {
    render(
      <MemoryRouter>
        <AccessDenied />
      </MemoryRouter>,
    )

    const heading = screen.getByRole('heading', {
      level: 1,
      name: /^access denied$/i,
    })
    expect(heading).toBeInTheDocument()

    expect(
      screen.getByText(/you do not have permission to access this section/i),
    ).toBeInTheDocument()

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('sets accessible document title to Access denied · HostelOS', () => {
    render(
      <MemoryRouter>
        <AccessDenied />
      </MemoryRouter>,
    )

    expect(document.title).toBe('Access denied · HostelOS')
  })

  it('renders an accessible back to overview link', () => {
    render(
      <MemoryRouter>
        <AccessDenied />
      </MemoryRouter>,
    )

    const overviewLink = screen.getByRole('link', {
      name: /back to overview/i,
    })
    expect(overviewLink).toBeInTheDocument()
    expect(overviewLink).toHaveAttribute('href', '/')
  })

  it('supports custom title, description, and action props', () => {
    render(
      <MemoryRouter>
        <AccessDenied
          title="Custom Forbidden View"
          description="Detailed custom restriction explanation."
          action={<button type="button">Request Permission</button>}
        />
      </MemoryRouter>,
    )

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Custom Forbidden View',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Detailed custom restriction explanation.'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Request Permission' }),
    ).toBeInTheDocument()
    expect(document.title).toBe('Custom Forbidden View · HostelOS')
  })
})
