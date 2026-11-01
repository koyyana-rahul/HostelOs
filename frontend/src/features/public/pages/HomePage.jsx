import { ArrowRight, Search, Building2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'

export default function HomePage() {
  return (
    <div>
      <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
        <div className="grid items-center gap-10 lg:grid-cols-2">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">HostelOS</p>
            <h1 className="mt-3 text-4xl font-semibold tracking-[-0.02em] text-ink sm:text-5xl">
              Discover hostels and PGs. Manage your property with ease.
            </h1>
            <p className="mt-4 text-lg text-muted">
              A two-sided platform connecting students and working professionals with verified accommodations, and giving hostel owners simple tools to run their business.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Button as={Link} to="/discover" size="lg">
                <Search className="size-4" />
                Find a hostel or PG
                <ArrowRight className="size-4" />
              </Button>
              <Button as={Link} to="/for-owners" variant="secondary" size="lg">
                <Building2 className="size-4" />
                List your property
              </Button>
            </div>
          </div>
          <Card className="p-6 sm:p-8">
            <p className="text-sm font-semibold text-ink">How it works</p>
            <div className="mt-4 space-y-4">
              <div className="flex items-start gap-3">
                <span className="mt-0.5 grid size-8 place-items-center rounded-full bg-brand-soft text-brand">1</span>
                <div>
                  <p className="font-medium text-ink">Discover</p>
                  <p className="text-sm text-muted">Browse published listings by location and preferences.</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <span className="mt-0.5 grid size-8 place-items-center rounded-full bg-brand-soft text-brand">2</span>
                <div>
                  <p className="font-medium text-ink">Connect</p>
                  <p className="text-sm text-muted">Sign in and submit an enquiry to the property owner.</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <span className="mt-0.5 grid size-8 place-items-center rounded-full bg-brand-soft text-brand">3</span>
                <div>
                  <p className="font-medium text-ink">Manage</p>
                  <p className="text-sm text-muted">Owners digitize operations with HostelOS Manage.</p>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </section>
    </div>
  )
}
