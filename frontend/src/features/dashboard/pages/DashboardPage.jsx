import {
  ArrowRight,
  BedDouble,
  Building2,
  CheckCircle2,
  Layers,
  TrendingUp,
  Users,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { buttonStyles } from '../../../components/ui/buttonStyles.js'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { ActivityFeed } from '../components/ActivityFeed.jsx'
import { MetricCard } from '../components/MetricCard.jsx'
import { PropertyOccupancyTable } from '../components/PropertyOccupancyTable.jsx'
import { mockDashboardData } from '../mock/mockDashboardData.js'

export function DashboardPage() {
  useDocumentTitle('Overview')

  const { metrics, properties, recentActivity } = mockDashboardData

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
            Module 0.1.3
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">
            Welcome to HostelOS
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">
            Operational overview for hostel and residence facilities. Real-time property
            metrics, occupancy tracking, and recent resident movements.
          </p>
        </div>
        <Link
          to="/foundation"
          className={buttonStyles({ variant: 'secondary' })}
        >
          Review foundation
          <ArrowRight className="size-4" aria-hidden="true" />
        </Link>
      </header>

      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-surface px-4 py-2.5 text-xs text-muted shadow-xs">
        <div className="flex items-center gap-2">
          <span className="size-2 rounded-full bg-accent" aria-hidden="true" />
          <span className="font-medium text-ink">
            Preview data — not sourced from live backend
          </span>
        </div>
        <p className="text-muted">
          Active residents ({metrics.activeResidents}) and occupied beds ({metrics.occupiedBeds}) differ: 2 assigned residents are temporarily off-site.
        </p>
      </div>

      <section aria-label="Key metrics">
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard
            icon={Building2}
            label="Total Properties"
            value={metrics.totalProperties}
            helperText={`${metrics.totalProperties} active locations`}
          />
          <MetricCard
            icon={BedDouble}
            label="Total Capacity"
            value={metrics.totalBeds}
            helperText={`${metrics.occupiedBeds} occupied · ${metrics.availableBeds} available`}
          />
          <MetricCard
            icon={TrendingUp}
            label="Occupancy Rate"
            value={`${metrics.occupancyRate.toFixed(1)}%`}
            badge="Healthy"
            badgeTone="success"
            helperText={`${metrics.pendingCheckIns} check-ins pending`}
          />
          <MetricCard
            icon={Users}
            label="Active Residents"
            value={metrics.activeResidents}
            helperText={`${metrics.occupiedBeds} beds currently assigned`}
          />
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-[1.25fr_0.75fr]">
        <div className="space-y-4">
          <Card className="p-6">
            <div className="flex items-center justify-between gap-3 pb-4">
              <div>
                <h2 className="text-base font-semibold text-ink">
                  Property Occupancy
                </h2>
                <p className="text-xs text-muted">
                  Capacity and live occupancy across all configured properties.
                </p>
              </div>
              <span className="text-xs text-muted">
                {metrics.totalProperties} properties
              </span>
            </div>
            <PropertyOccupancyTable properties={properties} />
          </Card>
        </div>

        <div className="space-y-4">
          <Card className="p-6">
            <div className="flex items-center justify-between gap-3 pb-4">
              <div>
                <h2 className="text-base font-semibold text-ink">
                  Recent Activity
                </h2>
                <p className="text-xs text-muted">
                  Latest check-ins, check-outs, and facility events.
                </p>
              </div>
              <span className="text-xs text-muted">
                {metrics.pendingCheckIns} in · {metrics.pendingCheckOuts} out pending
              </span>
            </div>
            <ActivityFeed activities={recentActivity} />
          </Card>
        </div>
      </div>

      <Card className="p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3.5">
            <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-soft text-brand">
              <Layers className="size-5" aria-hidden="true" />
            </span>
            <div>
              <h2 className="text-base font-semibold text-ink">
                Foundation Onboarding & Readiness
              </h2>
              <p className="mt-1 text-xs text-muted">
                HostelOS foundation provides responsive shells, authenticated sessions,
                role awareness, and robust UI primitives. Live operational endpoints
                will bind to this interface in upcoming modules.
              </p>
              <ul className="mt-3 flex flex-wrap gap-x-6 gap-y-2 text-xs text-muted">
                <li className="flex items-center gap-1.5">
                  <CheckCircle2 className="size-3.5 text-success" aria-hidden="true" />
                  Role contracts active
                </li>
                <li className="flex items-center gap-1.5">
                  <CheckCircle2 className="size-3.5 text-success" aria-hidden="true" />
                  Route error boundary wired
                </li>
                <li className="flex items-center gap-1.5">
                  <CheckCircle2 className="size-3.5 text-success" aria-hidden="true" />
                  F1 primitives integrated
                </li>
              </ul>
            </div>
          </div>
          <Link
            to="/foundation"
            className={buttonStyles({ variant: 'secondary', className: 'shrink-0' })}
          >
            Review foundation
            <ArrowRight className="size-4" aria-hidden="true" />
          </Link>
        </div>
      </Card>
    </div>
  )
}

export default DashboardPage
