import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PropertyMediaManager } from '../../src/features/org/components/PropertyMediaManager.jsx'

const mocks = {
  list: vi.fn(),
  refetch: vi.fn(),
  selectPrimary: vi.fn(),
  reorder: vi.fn(),
  deleteMedia: vi.fn(),
  uploadIntent: vi.fn(),
  completeUpload: vi.fn(),
}

vi.mock('../../src/features/organizations/api/propertyMediaApi.js', () => ({
  useListPropertyMediaQuery: (...args) => mocks.list(...args),
  useSelectPrimaryMediaMutation: () => [mocks.selectPrimary, { isLoading: false }],
  useReorderPropertyMediaMutation: () => [mocks.reorder, { isLoading: false }],
  useDeletePropertyMediaMutation: () => [mocks.deleteMedia, { isLoading: false }],
  useCreateMediaUploadIntentMutation: () => [mocks.uploadIntent, { isLoading: false }],
  useCompleteMediaUploadMutation: () => [mocks.completeUpload, { isLoading: false }],
}))

describe('PropertyMediaManager', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.list.mockReturnValue({
      data: [],
      isLoading: false,
      isError: false,
      refetch: mocks.refetch,
    })
  })

  it('renders the empty gallery state', () => {
    render(<PropertyMediaManager organizationId={1} propertyId={2} />)

    expect(screen.getByText('No images uploaded yet.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /choose images/i })).toBeInTheDocument()
  })

  it('renders gallery actions and primary state', () => {
    mocks.list.mockReturnValue({
      data: [
        { id: 11, secureUrl: 'https://cdn.example/one.jpg', format: 'jpg', primary: true },
        { id: 12, secureUrl: 'https://cdn.example/two.jpg', format: 'jpg', primary: false },
      ],
      isLoading: false,
      isError: false,
      refetch: mocks.refetch,
    })

    render(<PropertyMediaManager organizationId={1} propertyId={2} />)

    expect(screen.getByText('Primary')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /make primary/i })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /move image/i })).toHaveLength(4)
    expect(screen.getAllByRole('button', { name: /delete image/i })).toHaveLength(2)
  })

  it('shows a validation error for unsupported files', () => {
    render(<PropertyMediaManager organizationId={1} propertyId={2} />)

    const input = document.querySelector('input[type="file"]')
    const file = new File(['not-an-image'], 'notes.txt', { type: 'text/plain' })
    fireEvent.change(input, { target: { files: [file] } })

    expect(screen.getByText('Use JPEG, PNG, or WebP images.')).toBeInTheDocument()
  })
})
