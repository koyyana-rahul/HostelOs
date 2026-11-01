import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  ListingCardImage,
  ListingImageGallery,
} from '../../src/features/public/components/ListingImageGallery.jsx'

const listing = {
  title: 'Sunny Hostel',
  primaryImage: {
    url: 'https://cdn.example/primary.jpg',
    altText: 'Sunny Hostel',
    displayOrder: 0,
  },
  images: [
    { url: 'https://cdn.example/primary.jpg', altText: 'Sunny Hostel', displayOrder: 0 },
    { url: 'https://cdn.example/second.jpg', altText: 'Sunny Hostel', displayOrder: 1 },
  ],
}

describe('ListingImageGallery', () => {
  it('renders the primary card image and no-image fallback', () => {
    render(<ListingCardImage listing={listing} />)
    expect(screen.getByAltText('Sunny Hostel')).toHaveAttribute('src', listing.primaryImage.url)

    render(<ListingCardImage listing={{ title: 'Empty stay' }} />)
    expect(screen.getByText('No image available')).toBeInTheDocument()
  })

  it('supports thumbnail and next-image selection', () => {
    render(<ListingImageGallery listing={listing} />)

    expect(screen.getByAltText('Sunny Hostel')).toHaveAttribute('src', listing.images[0].url)
    fireEvent.click(screen.getByRole('button', { name: 'Next image' }))
    expect(screen.getByAltText('Sunny Hostel')).toHaveAttribute('src', listing.images[1].url)
    fireEvent.click(screen.getByRole('button', { name: 'View image 1' }))
    expect(screen.getByAltText('Sunny Hostel')).toHaveAttribute('src', listing.images[0].url)
  })

  it('shows a fallback after the main image fails', () => {
    render(<ListingImageGallery listing={listing} />)
    fireEvent.error(screen.getByAltText('Sunny Hostel'))
    expect(screen.getByText('No image available')).toBeInTheDocument()
  })
})
