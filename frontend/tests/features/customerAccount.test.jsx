import { fireEvent, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderApp } from '../test-utils.jsx'

const mocks = {
  profile: vi.fn(),
  updateProfile: vi.fn(),
  enquiries: vi.fn(),
  enquiry: vi.fn(),
}

vi.mock('../../src/features/customer/api/customerApi.js', () => ({
  useGetMyProfileQuery: (...args) => mocks.profile(...args),
  useUpdateMyProfileMutation: () => [mocks.updateProfile, { isLoading: false }],
  useListMyEnquiriesQuery: (...args) => mocks.enquiries(...args),
  useGetEnquiryQuery: (...args) => mocks.enquiry(...args),
}))

const customer = {
  id: 8,
  loginIdentifier: 'customer@example.com',
  displayName: 'Customer One',
  roles: ['CUSTOMER'],
}

const profile = { ...customer }

const enquiry = {
  id: 11,
  listingId: 20,
  listingTitle: 'Sunrise Hostel',
  organizationName: 'Sunrise Stays',
  message: 'Please share availability.',
  status: 'NEW',
  createdAt: '2026-10-31T10:00:00',
}

describe('customer account pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.profile.mockReturnValue({
      data: profile,
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    })
    mocks.enquiries.mockReturnValue({
      data: { content: [enquiry] },
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    })
    mocks.enquiry.mockReturnValue({
      data: enquiry,
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    })
    mocks.updateProfile.mockReturnValue({
      unwrap: () => Promise.resolve({ ...profile, displayName: 'Updated Customer' }),
    })
  })

  it('loads and saves the customer profile', async () => {
    renderApp({ route: '/account/profile', user: customer })

    const input = await screen.findByLabelText('Display name')
    fireEvent.change(input, { target: { value: 'Updated Customer' } })
    fireEvent.submit(input.closest('form'))

    expect(await screen.findByRole('status')).toHaveTextContent('Profile updated.')
    expect(mocks.updateProfile).toHaveBeenCalledWith({
      displayName: 'Updated Customer',
    })
  })

  it('renders profile errors and retry action', async () => {
    const refetch = vi.fn()
    mocks.profile.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: { data: { message: 'Profile unavailable' } },
      refetch,
    })

    renderApp({ route: '/account/profile', user: customer })

    expect(await screen.findByText('Profile unavailable')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(refetch).toHaveBeenCalled()
  })

  it('renders profile save errors', async () => {
    mocks.updateProfile.mockReturnValue({
      unwrap: () => Promise.reject({ data: { message: 'Profile update failed' } }),
    })

    renderApp({ route: '/account/profile', user: customer })

    const input = await screen.findByLabelText('Display name')
    fireEvent.change(input, { target: { value: 'Updated Customer' } })
    fireEvent.submit(input.closest('form'))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Profile update failed',
    )
  })

  it('renders enquiry history and links to details', async () => {
    renderApp({ route: '/my-enquiries', user: customer })

    expect(await screen.findByText('Please share availability.')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Sunrise Hostel' })).toHaveAttribute(
      'href',
      '/my-enquiries/11',
    )
  })

  it('renders empty and error enquiry states', async () => {
    mocks.enquiries.mockReturnValueOnce({
      data: { content: [] },
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    })
    renderApp({ route: '/my-enquiries', user: customer })
    expect(await screen.findByText('No enquiries yet')).toBeInTheDocument()
  })

  it('renders enquiry list errors and retry', async () => {
    const refetch = vi.fn()
    mocks.enquiries.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: { data: { message: 'Enquiries unavailable' } },
      refetch,
    })

    renderApp({ route: '/my-enquiries', user: customer })

    expect(await screen.findByText('Enquiries unavailable')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(refetch).toHaveBeenCalled()
  })

  it('renders enquiry detail failures and retry', async () => {
    const refetch = vi.fn()
    mocks.enquiry.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: { data: { message: 'Enquiry unavailable' } },
      refetch,
    })

    renderApp({ route: '/my-enquiries/11', user: customer })

    expect(await screen.findByText('Enquiry unavailable')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(refetch).toHaveBeenCalled()
  })
})
