import { Link } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'

export default function ForOwnersPage() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="max-w-2xl">
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">For Owners</p>
        <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">Digitize and manage your property</h1>
        <p className="mt-3 text-lg text-muted">
          HostelOS Manage helps you organize properties, rooms, staff, and operations in one secure workspace.
        </p>
        <div className="mt-6">
          <Button asChild>
            <Link to="/register">Get started as an owner</Link>
          </Button>
        </div>
      </div>
      <div className="mt-8 grid gap-4 md:grid-cols-2">
        <Card className="p-6">
          <p className="font-semibold text-ink">What you get</p>
          <ul className="mt-3 space-y-2 text-sm text-muted">
            <li className="flex items-center gap-2"><CheckCircle2 className="size-4 text-brand" /> Organization onboarding</li>
            <li className="flex items-center gap-2"><CheckCircle2 className="size-4 text-brand" /> Role-based access for your team</li>
            <li className="flex items-center gap-2"><CheckCircle2 className="size-4 text-brand" /> Manage properties and operations</li>
          </ul>
        </Card>
        <Card className="p-6">
          <p className="font-semibold text-ink">Get started</p>
          <p className="mt-2 text-sm text-muted">Create an account, set up your organization, and start managing your property.</p>
        </Card>
      </div>
    </div>
  )
}
