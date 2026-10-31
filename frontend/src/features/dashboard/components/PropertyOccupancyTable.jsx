import { Badge } from '../../../components/ui/Badge.jsx'
import {
  Table,
  TableBody,
  TableCell,
  TableEmptyState,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../components/ui/Table.jsx'

export function PropertyOccupancyTable({ properties = [] }) {
  return (
    <Table aria-label="Property occupancy overview">
      <TableHeader>
        <TableRow>
          <TableHead scope="col">Property</TableHead>
          <TableHead scope="col">Capacity</TableHead>
          <TableHead scope="col">Available</TableHead>
          <TableHead scope="col">Occupancy</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {properties.length === 0 ? (
          <TableEmptyState
            colSpan={4}
            title="No properties found"
            description="No property records configured in preview data."
          />
        ) : (
          properties.map((property) => (
            <TableRow key={property.id}>
              <TableCell>
                <div className="font-semibold text-ink">{property.name}</div>
                <div className="text-xs text-muted">{property.location}</div>
              </TableCell>
              <TableCell>
                <span className="font-medium text-ink">
                  {property.occupiedBeds}
                </span>{' '}
                <span className="text-muted">/ {property.totalBeds} beds</span>
              </TableCell>
              <TableCell>
                <span className="text-ink">{property.availableBeds} beds</span>
              </TableCell>
              <TableCell>
                <Badge tone={property.tone} dot>
                  {property.occupancyRate.toFixed(1)}%
                </Badge>
              </TableCell>
            </TableRow>
          ))
        )}
      </TableBody>
    </Table>
  )
}
