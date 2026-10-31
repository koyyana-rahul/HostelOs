import { Building2, Construction } from 'lucide-react'
import { Card } from '../../../components/ui/Card.jsx'

export default function OrgPropertiesPage() {
  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">Properties</h1>
        <p className="mt-1 text-sm text-muted">Manage hostel properties</p>
      </header>
      <Card className="p-6">
        <div className="flex items-center gap-3">
          <span className="grid size-10 place-items-center rounded-xl bg-brand-soft">
            <Construction className="size-4 text-brand" aria-hidden="true" />
          </span>
          <div>
            <p className="font-medium text-ink">Planned</p>
            <p className="text-sm text-muted">Property management will be added in upcoming phases</p>
          </div>
        </div>
      </Card>
    </div>
  )
}
