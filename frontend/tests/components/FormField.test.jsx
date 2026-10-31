import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { FormField } from '../../src/components/ui/FormField.jsx'
import { Input } from '../../src/components/ui/Input.jsx'

describe('FormField component', () => {
  it('associates label with child input via generated ID', () => {
    render(
      <FormField label="Full Name">
        <Input />
      </FormField>,
    )

    const input = screen.getByLabelText('Full Name')
    expect(input).toBeInTheDocument()
    expect(input.id).toBeTruthy()
  })

  it('preserves caller-provided ID on FormField', () => {
    render(
      <FormField id="custom-email-id" label="Email Address">
        <Input />
      </FormField>,
    )

    const input = screen.getByLabelText('Email Address')
    expect(input).toHaveAttribute('id', 'custom-email-id')
  })

  it('renders required indicator when required is true', () => {
    render(
      <FormField label="Password" required>
        <Input type="password" />
      </FormField>,
    )

    expect(screen.getByText('*')).toHaveAttribute('aria-hidden', 'true')
    const input = screen.getByLabelText(/password/i)
    expect(input).toBeRequired()
  })

  it('associates hint text with input via aria-describedby', () => {
    render(
      <FormField
        id="user-code"
        label="User Code"
        hint="Must be 6 characters long."
      >
        <Input />
      </FormField>,
    )

    const input = screen.getByLabelText('User Code')
    expect(input).toHaveAttribute('aria-describedby', 'user-code-hint')
    expect(screen.getByText('Must be 6 characters long.')).toHaveAttribute(
      'id',
      'user-code-hint',
    )
  })

  it('associates error text with role="alert" and invalid state', () => {
    render(
      <FormField
        id="phone-number"
        label="Phone Number"
        error="Invalid telephone number format."
      >
        <Input />
      </FormField>,
    )

    const input = screen.getByLabelText('Phone Number')
    expect(input).toHaveAttribute('aria-describedby', 'phone-number-error')
    expect(input).toHaveAttribute('aria-invalid', 'true')

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('Invalid telephone number format.')
    expect(alert).toHaveAttribute('id', 'phone-number-error')
  })

  it('merges both hint and error into aria-describedby when both are present', () => {
    render(
      <FormField
        id="room-num"
        label="Room Number"
        hint="Floor 1-4 only."
        error="Room is already occupied."
      >
        <Input />
      </FormField>,
    )

    const input = screen.getByLabelText('Room Number')
    expect(input).toHaveAttribute(
      'aria-describedby',
      'room-num-hint room-num-error',
    )
    expect(screen.getByText('Floor 1-4 only.')).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('Room is already occupied.')
  })

  it('preserves caller-supplied aria-describedby and deduplicates tokens', () => {
    render(
      <FormField
        id="doc-id"
        label="Document ID"
        hint="Format: DOC-XXXX."
      >
        <Input aria-describedby="external-desc doc-id-hint" />
      </FormField>,
    )

    const input = screen.getByLabelText('Document ID')
    expect(input).toHaveAttribute(
      'aria-describedby',
      'external-desc doc-id-hint',
    )
  })

  it('supports child render function', () => {
    render(
      <FormField
        id="render-fn-field"
        label="Custom Field"
        hint="Helper details."
        error="Problem found."
      >
        {(fieldProps) => (
          <Input
            {...fieldProps}
            data-testid="custom-rendered-input"
          />
        )}
      </FormField>,
    )

    const input = screen.getByTestId('custom-rendered-input')
    expect(input).toHaveAttribute('id', 'render-fn-field')
    expect(input).toHaveAttribute(
      'aria-describedby',
      'render-fn-field-hint render-fn-field-error',
    )
  })

  it('renders children as-is when multiple siblings are provided without a single wrapper', () => {
    render(
      <FormField id="multi-child" label="Multi Child Field">
        <span data-testid="child-1">First</span>
        <span data-testid="child-2">Second</span>
      </FormField>,
    )

    expect(screen.getByText('Multi Child Field')).toBeInTheDocument()
    expect(screen.getByTestId('child-1')).toBeInTheDocument()
    expect(screen.getByTestId('child-2')).toBeInTheDocument()
  })
})
