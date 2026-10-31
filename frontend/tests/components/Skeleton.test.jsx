import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Skeleton } from '../../src/components/ui/Skeleton.jsx'

describe('Skeleton component', () => {
  it('renders text variant by default with aria-hidden="true"', () => {
    const { container } = render(<Skeleton />)
    const skeleton = container.firstChild
    expect(skeleton).toHaveAttribute('aria-hidden', 'true')
    expect(skeleton.className).toContain('h-4')
    expect(skeleton.className).toContain('w-full')
    expect(skeleton.className).toContain('rounded')
    expect(skeleton.className).toContain('animate-pulse')
    expect(skeleton.className).toContain('motion-reduce:animate-none')
    expect(skeleton.className).toContain('bg-muted/15')
  })

  it('renders circular variant', () => {
    const { container } = render(
      <Skeleton variant="circular" className="size-12" />,
    )
    const skeleton = container.firstChild
    expect(skeleton).toHaveAttribute('aria-hidden', 'true')
    expect(skeleton.className).toContain('rounded-full')
    expect(skeleton.className).toContain('size-12')
    expect(skeleton.className).toContain('animate-pulse')
  })

  it('renders rectangular variant', () => {
    const { container } = render(
      <Skeleton variant="rectangular" className="h-48 w-full" />,
    )
    const skeleton = container.firstChild
    expect(skeleton).toHaveAttribute('aria-hidden', 'true')
    expect(skeleton.className).toContain('rounded-xl')
    expect(skeleton.className).toContain('h-48')
    expect(skeleton.className).toContain('w-full')
  })

  it('preserves additional class names passed by consumer', () => {
    const { container } = render(
      <Skeleton className="my-2 max-w-sm" />,
    )
    const skeleton = container.firstChild
    expect(skeleton.className).toContain('my-2')
    expect(skeleton.className).toContain('max-w-sm')
  })
})
