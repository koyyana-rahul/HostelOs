import { Link } from 'react-router-dom'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'

export default function DiscoverPage() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="max-w-2xl">
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">Discover</p>
        <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">Find hostels and PGs</h1>
        <p className="mt-3 text-lg text-muted">
          Browse published accommodations. Only published listings are shown in public discovery.
        </p>
      </div>
      <Card className="mt-8 p-6 sm:p-8">
        <p className="text-sm font-semibold text-ink">Listings coming soon</p>
        <p className="mt-2 text-sm text-muted">
          Public discovery is ready. Listings will appear here once published by property owners.
        </p>
        <div className="mt-4">
          <Button asChild variant="secondary">
            <Link to="/register">Create an account to enquire</Link>
          </Button>
        </div>
      </Card>
    </div>
  )
}
