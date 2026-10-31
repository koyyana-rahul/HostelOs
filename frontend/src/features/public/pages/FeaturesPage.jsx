import { Card } from '../../../components/ui/Card.jsx'

export default function FeaturesPage() {
  const features = [
    { title: 'Discover', desc: 'Public discovery for hostels and PGs with clear listings.' },
    { title: 'Enquiries', desc: 'Connect tenants with owners through secure enquiries.' },
    { title: 'Manage', desc: 'Organize properties, rooms, staff, and operations.' },
    { title: 'Access control', desc: 'Role-based permissions across platform and organizations.' },
    { title: 'Secure auth', desc: 'Session-based authentication with CSRF protection.' },
    { title: 'Organization isolation', desc: 'Data scoped by organization with proper authorization.' },
  ]

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="max-w-2xl">
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">Features</p>
        <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">Built for discovery and operations</h1>
        <p className="mt-3 text-lg text-muted">Everything you need to find accommodation or run your property.</p>
      </div>
      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {features.map((f) => (
          <Card key={f.title} className="p-6">
            <p className="font-semibold text-ink">{f.title}</p>
            <p className="mt-2 text-sm text-muted">{f.desc}</p>
          </Card>
        ))}
      </div>
    </div>
  )
}
