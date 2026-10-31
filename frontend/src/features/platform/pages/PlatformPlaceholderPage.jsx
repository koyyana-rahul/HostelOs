import { Construction } from 'lucide-react'
import { Card } from '../../../components/ui/Card.jsx'

export default function PlatformPlaceholderPage() {
  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">Planned</h1>
        <p className="mt-1 text-sm text-muted">This module is not yet implemented</p>
      </header>
      <Card className="p-6">
        <div className="flex items-center gap-3">
          <span className="grid size-10 place-items-center rounded-xl bg-brand-soft">
            <Construction className="size-4 text-brand" aria-hidden="true" />
          </span>
          <div>
            <p className="font-medium text-ink">Not yet implemented</p>
            <p className="text-sm text-muted">Backend APIs for this functionality do not exist yet.</p>
          </div>
        </div>
      </Card>
    </div>
  )
}
