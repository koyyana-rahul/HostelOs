import { Construction } from 'lucide-react'
import { Card } from '../../../components/ui/Card.jsx'

export default function OrgStaffPage() {
  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">Staff and Members</h1>
        <p className="mt-1 text-sm text-muted">Manage organization members</p>
      </header>
      <Card className="p-6">
        <div className="flex items-center gap-3">
          <span className="grid size-10 place-items-center rounded-xl bg-brand-soft">
            <Construction className="size-4 text-brand" aria-hidden="true" />
          </span>
          <div>
            <p className="font-medium text-ink">Planned</p>
            <p className="text-sm text-muted">Member management UI available via API; interface coming soon</p>
          </div>
        </div>
      </Card>
    </div>
  )
}
