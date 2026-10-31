import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  csrfPayload,
  jsonResponse,
  staffUserPayload,
  stubFetchWithMemberships,
} from '../helpers/api.js'
import {
  authenticatedUser,
  renderApp,
  staffUser,
} from '../test-utils.jsx'

describe('finalized role contract', () => {
  it('renders exactly the platform, organization, and staff role names', async () => {
    renderApp({
      route: '/foundation',
      user: {
        ...authenticatedUser,
        roles: ['PLATFORM_ADMIN', 'ORGANIZATION_ADMIN', 'STAFF'],
      },
    })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const roles = screen.getByRole('list', {
      name: /server-assigned roles/i,
    })

    expect(within(roles).getAllByRole('listitem')).toHaveLength(3)
    expect(within(roles).getByText('PLATFORM_ADMIN')).toBeInTheDocument()
    expect(within(roles).getByText('ORGANIZATION_ADMIN')).toBeInTheDocument()
    expect(within(roles).getByText('STAFF')).toBeInTheDocument()
    expect(
      screen.getByText(
        /informational only; access is enforced by the backend/i,
      ),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /administration/i }),
    ).not.toBeInTheDocument()
  })

  it('does not derive navigation or actions from the STAFF role', async () => {
    renderApp({ route: '/foundation', user: staffUser })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const navigation = screen.getByRole('navigation', { name: /primary/i })

    expect(
      within(navigation)
        .getAllByRole('link')
        .map((link) => link.textContent),
    ).toEqual(['Overview', 'Foundation', 'System'])
    expect(within(navigation).queryByText(/admin/i)).not.toBeInTheDocument()
    expect(screen.getByText('STAFF')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign out/i })).toBeInTheDocument()
  })

  it('preserves the sign-in flow for a STAFF identity', async () => {
    stubFetchWithMemberships(
      [
        {
          id: 1,
          organizationId: 10,
          organizationName: 'Green Valley Hostel',
          orgRole: 'ORG_STAFF',
          status: 'ACTIVE',
        },
      ],
      jsonResponse(csrfPayload),
      jsonResponse(staffUserPayload),
      jsonResponse(staffUserPayload),
    )
    const user = userEvent.setup()

    renderApp({ route: '/login', authStatus: 'anonymous' })

    await user.type(
      await screen.findByLabelText(/email or username/i),
      staffUserPayload.loginIdentifier,
    )
    await user.type(
      screen.getByLabelText(/^password$/i),
      'correct-horse-battery-staple',
    )
    await user.click(screen.getByRole('button', { name: /^sign in$/i }))

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: /^overview$/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText(staffUserPayload.displayName)).toBeInTheDocument()
    expect(screen.getByText('STAFF')).toBeInTheDocument()
    expect(screen.queryByText('PLATFORM_ADMIN')).not.toBeInTheDocument()
    expect(screen.queryByText('ORGANIZATION_ADMIN')).not.toBeInTheDocument()
  })
})
