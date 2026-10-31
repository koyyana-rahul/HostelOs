import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createRef, useState } from 'react'
import { describe, expect, it } from 'vitest'
import { Input } from '../../src/components/ui/Input.jsx'

describe('Input component', () => {
  it('renders a default text input', () => {
    render(<Input placeholder="Enter your name" />)
    const input = screen.getByPlaceholderText('Enter your name')
    expect(input).toBeInTheDocument()
    expect(input).toHaveAttribute('type', 'text')
    expect(input).not.toHaveAttribute('aria-invalid')
  })

  it('supports controlled input with onChange', async () => {
    const user = userEvent.setup()

    function ControlledHarness() {
      const [value, setValue] = useState('')
      return (
        <Input
          aria-label="Username"
          value={value}
          onChange={(e) => setValue(e.target.value)}
        />
      )
    }

    render(<ControlledHarness />)
    const input = screen.getByLabelText('Username')
    await user.type(input, 'admin')
    expect(input).toHaveValue('admin')
  })

  it('supports uncontrolled input with defaultValue', async () => {
    const user = userEvent.setup()
    render(<Input aria-label="Nickname" defaultValue="Alex" />)
    const input = screen.getByLabelText('Nickname')
    expect(input).toHaveValue('Alex')
    await user.clear(input)
    await user.type(input, 'HostelAdmin')
    expect(input).toHaveValue('HostelAdmin')
  })

  it('handles invalid prop by applying aria-invalid and danger styles', () => {
    render(<Input aria-label="Postal Code" invalid />)
    const input = screen.getByLabelText('Postal Code')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input.className).toContain('border-danger')
  })

  it('handles aria-invalid prop explicitly', () => {
    render(<Input aria-label="Tax ID" aria-invalid="true" />)
    const input = screen.getByLabelText('Tax ID')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input.className).toContain('border-danger')
  })

  it('handles disabled state', () => {
    render(<Input aria-label="System Key" disabled />)
    const input = screen.getByLabelText('System Key')
    expect(input).toBeDisabled()
    expect(input.className).toContain('disabled:cursor-not-allowed')
  })

  it('handles readOnly state', () => {
    render(<Input aria-label="System ID" value="SYS-1234" readOnly />)
    const input = screen.getByLabelText('System ID')
    expect(input).toHaveAttribute('readonly')
  })

  it('forwards ref properly', () => {
    const ref = createRef()
    render(<Input ref={ref} aria-label="Ref target" />)
    expect(ref.current).toBeInstanceOf(HTMLInputElement)
  })

  it('merges custom class names without dropping base styles', () => {
    render(<Input aria-label="Custom" className="max-w-xs" />)
    const input = screen.getByLabelText('Custom')
    expect(input.className).toContain('max-w-xs')
    expect(input.className).toContain('rounded-lg')
  })
})
