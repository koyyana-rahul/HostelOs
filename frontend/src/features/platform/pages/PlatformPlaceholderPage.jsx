import { Construction } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { Card } from '../../../components/ui/Card.jsx'
import { Badge } from '../../../components/ui/Badge.jsx'

const moduleInfo = {
  '/platform/subscriptions': {
    title: 'Subscription Management',
    description: 'Backend APIs for subscription management do not exist yet.',
  },
  '/platform/settings': {
    title: 'Platform Settings',
    description: 'Backend APIs for platform settings do not exist yet.',
  },
}

export default function PlatformPlaceholderPage() {
  const location = useLocation()
  const info = moduleInfo[location.pathname] ?? {
    title: 'Planned',
    description: 'This module is not yet implemented.',
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">{info.title}</h1>
        <p className="mt-1 text-sm text-muted">Planned for a future module</p>
      </header>
      <Card className="p-6">
        <div className="flex items-center gap-3">
          <span className="grid size-10 place-items-center rounded-xl bg-brand-soft">
            <Construction className="size-4 text-brand" aria-hidden="true" />
          </span>
          <div>
            <p className="font-medium text-ink">Not yet implemented</p>
            <p className="text-sm text-muted">{info.description}</p>
          </div>
        </div>
      </Card>
      <Card className="p-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-ink">Current plan</p>
            <p className="mt-1 text-sm text-muted">
              HostelOS is currently operating under a free access model. No membership or
              subscription fees are charged during the free-launch period.
            </p>
          </div>
          <Badge tone="success">FREE · ₹0</Badge>
        </div>
      </Card>
    </div>
  )
}