import {
  ArrowRight,
  Building2,
  CheckCircle2,
  ClipboardList,
  MessageSquare,
  Search,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { hasOrganizationRole } from '../../auth/lib/roleResolver.js'

const benefits = [
  {
    icon: Building2,
    title: 'One place for every property',
    description:
      'Keep hostels, PGs, and rooms organized under your organization.',
  },
  {
    icon: Search,
    title: 'Publish to discovery',
    description:
      'Draft a listing, publish it when complete, and appear in customer search.',
  },
  {
    icon: MessageSquare,
    title: 'Respond to enquiries',
    description:
      'See every customer message, track its status, and follow up from one inbox.',
  },
]

const steps = [
  {
    icon: CheckCircle2,
    title: 'Create an owner account',
    description: 'Register with your email and sign in securely.',
  },
  {
    icon: Building2,
    title: 'Set up your organization',
    description: 'Add your organization once and start configuring properties.',
  },
  {
    icon: ClipboardList,
    title: 'Publish and manage',
    description: 'Go live with listings and manage enquiries day to day.',
  },
]

export default function ForOwnersPage() {
  const user = useAppSelector(selectCurrentUser)
  const isOwner = hasOrganizationRole(user)

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <section className="grid items-center gap-10 lg:grid-cols-[1.2fr_1fr]">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
            For owners
          </p>
          <h1 className="mt-3 text-4xl font-semibold leading-tight tracking-[-0.03em] text-ink sm:text-5xl">
            Run your hostel from one calm workspace.
          </h1>
          <p className="mt-5 max-w-xl text-lg leading-8 text-muted">
            HostelOS Manage brings your properties, listings, and customer
            enquiries together, so you can spend less time on spreadsheets and
            more time hosting.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Button
              as={Link}
              to={isOwner ? '/app/overview' : '/register?type=owner'}
              size="lg"
            >
              {isOwner ? 'Open your workspace' : 'Create an owner account'}
              <ArrowRight className="size-4" aria-hidden="true" />
            </Button>
            <Button as={Link} to="/discover" variant="secondary" size="lg">
              See public listings
            </Button>
          </div>
          <p className="mt-3 text-xs text-muted">
            Already registered?{' '}
            <Link to="/login" className="text-brand hover:underline">
              Sign in to your owner account
            </Link>
          </p>
        </div>

        <Card className="p-6">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted">
            What you can manage
          </p>
          <ul className="mt-4 space-y-3">
            {benefits.map(({ icon: Icon, title, description }) => (
              <li key={title} className="flex gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-brand-soft text-brand">
                  <Icon className="size-4" aria-hidden="true" />
                </span>
                <div>
                  <p className="text-sm font-semibold text-ink">{title}</p>
                  <p className="mt-0.5 text-xs leading-5 text-muted">
                    {description}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      </section>

      <section className="mt-16">
        <h2 className="text-2xl font-semibold tracking-[-0.02em] text-ink">
          How it works
        </h2>
        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {steps.map(({ icon: Icon, title, description }, index) => (
            <Card key={title} className="p-6">
              <div className="flex items-center gap-2">
                <span className="grid size-8 place-items-center rounded-lg bg-ink text-xs font-semibold text-white">
                  {index + 1}
                </span>
                <Icon className="size-4 text-brand" aria-hidden="true" />
              </div>
              <h3 className="mt-4 text-sm font-semibold text-ink">{title}</h3>
              <p className="mt-1 text-sm leading-6 text-muted">{description}</p>
            </Card>
          ))}
        </div>
      </section>

      <section className="mt-16 rounded-2xl border border-brand/15 bg-brand-soft/60 p-8 sm:p-10">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-xl font-semibold tracking-[-0.02em] text-ink">
              Ready to list your first property?
            </h2>
            <p className="mt-1 text-sm text-muted">
              Owner onboarding takes a few minutes. No setup fee.
            </p>
          </div>
          <Button
            as={Link}
            to={isOwner ? '/app/overview' : '/register?type=owner'}
          >
            {isOwner ? 'Go to overview' : 'Get started as an owner'}
          </Button>
        </div>
      </section>
    </div>
  )
}
