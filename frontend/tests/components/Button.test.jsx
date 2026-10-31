import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Link } from 'react-router-dom'
import { MemoryRouter } from 'react-router-dom'
import { Button } from '../../src/components/ui/Button.jsx'

describe('Button component', () => {
  it('renders a native button by default', () => {
    render(<Button>Click me</Button>)
    const button = document.querySelector('button')
    expect(button).toBeInTheDocument()
    expect(button).not.toHaveAttribute('aschild')
    expect(button).not.toHaveAttribute('as')
  })

  it('applies default variant and size classes', () => {
    render(<Button>Default</Button>)
    const button = document.querySelector('button')
    expect(button.className).toContain('bg-brand')
    expect(button.className).toContain('h-10')
  })

  it('applies secondary variant and sm size', () => {
    render(<Button variant="secondary" size="sm">Secondary</Button>)
    const button = document.querySelector('button')
    expect(button.className).toContain('border')
    expect(button.className).toContain('h-9')
  })

  it('applies ghost variant', () => {
    render(<Button variant="ghost">Ghost</Button>)
    const button = document.querySelector('button')
    expect(button.className).toContain('text-muted')
  })

  it('merges custom className without dropping base styles', () => {
    render(<Button className="max-w-xs">Custom</Button>)
    const button = document.querySelector('button')
    expect(button.className).toContain('max-w-xs')
    expect(button.className).toContain('rounded-lg')
  })

  it('preserves disabled state', () => {
    render(<Button disabled>Disabled</Button>)
    const button = document.querySelector('button')
    expect(button).toBeDisabled()
    expect(button.className).toContain('disabled:pointer-events-none')
  })

  it('preserves type attribute', () => {
    render(<Button type="submit">Submit</Button>)
    const button = document.querySelector('button')
    expect(button).toHaveAttribute('type', 'submit')
  })

  it('does not render an asChild DOM attribute', () => {
    render(<Button asChild>Slot</Button>)
    const button = document.querySelector('button')
    expect(button).not.toHaveAttribute('aschild')
  })

  it('renders as a Link when as={Link} is provided', () => {
    render(
      <MemoryRouter>
        <Button as={Link} to="/dashboard">Dashboard</Button>
      </MemoryRouter>,
    )
    const link = document.querySelector('a')
    expect(link).toBeInTheDocument()
    expect(link).toHaveAttribute('href', '/dashboard')
    expect(link.className).toContain('bg-brand')
  })

  it('preserves navigation when rendered as a Link', () => {
    render(
      <MemoryRouter>
        <Button as={Link} to="/login" variant="ghost" size="sm">Sign in</Button>
      </MemoryRouter>,
    )
    const link = document.querySelector('a')
    expect(link).toHaveAttribute('href', '/login')
    expect(link.className).toContain('text-muted')
    expect(link.className).toContain('h-9')
  })
})