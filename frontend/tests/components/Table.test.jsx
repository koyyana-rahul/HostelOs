import { render, screen } from '@testing-library/react'
import { Building2 } from 'lucide-react'
import { describe, expect, it } from 'vitest'
import {
  Table,
  TableBody,
  TableCell,
  TableEmptyState,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
} from '../../src/components/ui/Table.jsx'

describe('Table component', () => {
  it('renders a semantic table structure with headers and rows', () => {
    render(
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Hostel Name</TableHead>
            <TableHead className="text-right">Total Beds</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell>Green Valley</TableCell>
            <TableCell className="text-right">80</TableCell>
          </TableRow>
          <TableRow>
            <TableCell>Downtown Residency</TableCell>
            <TableCell className="text-right">60</TableCell>
          </TableRow>
        </TableBody>
        <TableFooter>
          <TableRow>
            <TableCell>Total</TableCell>
            <TableCell className="text-right">140</TableCell>
          </TableRow>
        </TableFooter>
      </Table>,
    )

    const table = screen.getByRole('table')
    expect(table).toBeInTheDocument()

    const headers = screen.getAllByRole('columnheader')
    expect(headers).toHaveLength(2)
    expect(headers[0]).toHaveTextContent('Hostel Name')
    expect(headers[0]).toHaveAttribute('scope', 'col')
    expect(headers[1]).toHaveClass('text-right')

    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(4) // 1 thead + 2 tbody + 1 tfoot
    expect(screen.getByText('Green Valley')).toBeInTheDocument()
    expect(screen.getByText('Downtown Residency')).toBeInTheDocument()
    expect(screen.getByText('Total')).toBeInTheDocument()
  })

  it('renders TableEmptyState spanning the table with colSpan', () => {
    render(
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Hostel</TableHead>
            <TableHead>Location</TableHead>
            <TableHead>Status</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableEmptyState
            colSpan={3}
            icon={Building2}
            title="No properties found"
            description="You have not registered any hostels yet."
            action={<button type="button">Add property</button>}
          />
        </TableBody>
      </Table>,
    )

    const cell = screen.getByRole('cell')
    expect(cell).toHaveAttribute('colspan', '3')
    expect(
      screen.getByRole('heading', { level: 1, name: 'No properties found' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('You have not registered any hostels yet.'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Add property' }),
    ).toBeInTheDocument()
  })

  it('preserves custom class names on table container and rows', () => {
    const { container } = render(
      <Table className="custom-table">
        <TableBody>
          <TableRow className="custom-row">
            <TableCell className="custom-cell">Content</TableCell>
          </TableRow>
        </TableBody>
      </Table>,
    )

    expect(container.querySelector('.custom-table')).toBeInTheDocument()
    expect(container.querySelector('.custom-row')).toBeInTheDocument()
    expect(container.querySelector('.custom-cell')).toBeInTheDocument()
  })
})
