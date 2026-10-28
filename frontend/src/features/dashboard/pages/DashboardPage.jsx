import {
  ArrowRight,
  CheckCircle2,
  Database,
  LayoutDashboard,
  Server,
  ShieldCheck,
  Terminal,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { buttonStyles } from '../../../components/ui/buttonStyles.js'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { env } from '../../../lib/env.js'

const foundationItems = [
  'React 19 + Vite 8',
  'Tailwind CSS 4 theme',
  'React Router 7 shell',
  'Redux Toolkit store',
  'RTK Query base API',
  'Vitest + Testing Library',
  'Environment conventions',
  'Accessible feedback states',
]

const commands = [
  ['npm run dev', 'Start the local development server'],
  ['npm run test', 'Run the test suite once'],
  ['npm run build', 'Create a production build'],
  ['npm run lint', 'Check the codebase with ESLint'],
]

function StatusCard({ icon: Icon, title, description, status }) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-soft text-brand">
          <Icon className="size-5" aria-hidden="true" />
        </span>
        <span className="inline-flex items-center gap-1.5 pt-1 text-[11px] font-medium text-success">
          <span className="size-1.5 rounded-full bg-success" aria-hidden="true" />
          {status}
        </span>
      </div>
      <h2 className="mt-4 text-sm font-semibold text-ink">{title}</h2>
      <p className="mt-1 text-xs leading-5 text-muted">{description}</p>
    </Card>
  )
}

export default function DashboardPage() {
  useDocumentTitle('Overview')

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
            Module 0.1.1
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">
            Welcome to HostelOS
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">
            A clean starting point for the hostel operations platform. The
            shell, routing, state layer, and quality gates are in place so
            feature modules can be added incrementally.
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

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatusCard
          icon={LayoutDashboard}
          title="Application shell"
          description="Responsive navigation, layout, and route feedback are ready."
          status="Ready"
        />
        <StatusCard
          icon={Database}
          title="State layer"
          description="A predictable Redux Toolkit store powers shared application state."
          status="Wired"
        />
        <StatusCard
          icon={ShieldCheck}
          title="Quality gates"
          description="Vitest, React Testing Library, and ESLint are configured."
          status="Configured"
        />
        <StatusCard
          icon={Server}
          title="API boundary"
          description="RTK Query is configured with no business endpoints yet."
          status="Prepared"
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
        <Card className="p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold text-ink">
                Foundation checklist
              </h2>
              <p className="mt-1 text-xs text-muted">
                The essentials required by the next module.
              </p>
            </div>
            <span className="rounded-full bg-success-soft px-2.5 py-1 text-[11px] font-semibold text-success">
              Complete
            </span>
          </div>
          <ul className="mt-5 grid gap-x-6 gap-y-3 sm:grid-cols-2">
            {foundationItems.map((item) => (
              <li
                key={item}
                className="flex items-center gap-2.5 text-sm text-ink"
              >
                <CheckCircle2
                  className="size-4 shrink-0 text-success"
                  aria-hidden="true"
                />
                {item}
              </li>
            ))}
          </ul>
        </Card>

        <Card
          tone="brand"
          className="relative overflow-hidden"
        >
          <div
            className="surface-grid pointer-events-none absolute inset-0 opacity-60"
            aria-hidden="true"
          />
          <div className="relative flex h-full flex-col p-6">
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-white/55">
              Workspace status
            </p>
            <h2 className="mt-2 text-xl font-semibold tracking-tight">
              Ready for feature delivery
            </h2>
            <p className="mt-2 max-w-sm text-sm leading-6 text-white/70">
              The foundation is intentionally free of authentication and
              business logic. Add capabilities without reworking the shell.
            </p>
            <div className="mt-auto pt-6">
              <div className="rounded-xl border border-white/15 bg-white/5 p-4">
                <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-white/50">
                  API base URL
                </p>
                <p className="mt-1 break-all font-mono text-xs text-white/90">
                  {env.apiBaseUrl || '/'}
                </p>
              </div>
              <Link
                to="/foundation"
                className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-white transition-colors hover:text-white/75"
              >
                Foundation details
                <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
            </div>
          </div>
        </Card>
      </div>

      <Card className="p-6">
        <div className="flex items-center gap-3">
          <span className="grid size-9 place-items-center rounded-xl bg-canvas text-brand">
            <Terminal className="size-4" aria-hidden="true" />
          </span>
          <div>
            <h2 className="text-base font-semibold text-ink">
              Workspace commands
            </h2>
            <p className="text-xs text-muted">
              Run these from the frontend directory.
            </p>
          </div>
        </div>
        <dl className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {commands.map(([command, description]) => (
            <div
              key={command}
              className="rounded-xl border border-line bg-canvas/60 p-4"
            >
              <dt className="font-mono text-xs font-semibold text-ink">
                {command}
              </dt>
              <dd className="mt-1.5 text-xs leading-5 text-muted">
                {description}
              </dd>
            </div>
          ))}
        </dl>
      </Card>
    </div>
  )
}
