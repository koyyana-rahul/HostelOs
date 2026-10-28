import { FolderTree, Layers, Server, Terminal } from 'lucide-react'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { env } from '../../../lib/env.js'

const stack = [
  ['Interface', 'React 19 · Vite 8'],
  ['Styling', 'Tailwind CSS 4'],
  ['Routing', 'React Router 7'],
  ['State', 'Redux Toolkit 2 · React Redux 9'],
  ['Data', 'RTK Query'],
  ['Quality', 'Vitest 5 · React Testing Library'],
  ['DOM environment', 'jsdom 29'],
  ['Lint', 'ESLint 10'],
  ['Runtime', 'Node.js 22+'],
  ['Icons', 'Lucide React'],
]

const structure = [
  ['src/app', 'Providers, router, and Redux store'],
  ['src/components/ui', 'Reusable UI primitives'],
  ['src/components/layout', 'Application shell and navigation'],
  ['src/components/feedback', 'Loading, error, and empty states'],
  ['src/features', 'Business capabilities, added incrementally'],
  ['src/services/api', 'Shared RTK Query configuration'],
  ['src/hooks', 'Reusable React hooks'],
  ['src/lib', 'Environment and library configuration'],
  ['src/utils', 'Small reusable utilities'],
  ['src/styles', 'Global styling and design tokens'],
  ['tests', 'Test setup and application suites'],
]

const commands = [
  ['npm run dev', 'Start the development server'],
  ['npm run test', 'Run all tests once'],
  ['npm run test:watch', 'Watch tests during development'],
  ['npm run build', 'Create a production build'],
  ['npm run lint', 'Check the codebase with ESLint'],
]

const handoffSteps = [
  'Create the capability under src/features.',
  'Inject endpoints into the shared RTK Query API.',
  'Register the route in the application router.',
  'Add the navigation entry to the layout.',
  'Cover the new behavior with tests.',
]

export default function FoundationPage() {
  useDocumentTitle('Foundation')

  return (
    <div className="space-y-6">
      <header>
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
          Foundation
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">
          A clear base for every module
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">
          The conventions below keep HostelOS predictable as the product grows.
          This module stops at infrastructure: no authentication, no business
          rules, and no speculative feature code.
        </p>
      </header>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card className="p-6">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-xl bg-brand-soft text-brand">
              <Layers className="size-4" aria-hidden="true" />
            </span>
            <div>
              <h2 className="text-base font-semibold text-ink">
                Application stack
              </h2>
              <p className="text-xs text-muted">
                Compatible versions selected for this module.
              </p>
            </div>
          </div>
          <dl className="mt-5 divide-y divide-line/80">
            {stack.map(([area, tools]) => (
              <div
                key={area}
                className="flex items-center justify-between gap-4 py-2.5 first:pt-0 last:pb-0"
              >
                <dt className="text-xs font-medium uppercase tracking-[0.12em] text-muted">
                  {area}
                </dt>
                <dd className="text-right text-sm text-ink">{tools}</dd>
              </div>
            ))}
          </dl>
        </Card>

        <Card className="p-6">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-xl bg-brand-soft text-brand">
              <FolderTree className="size-4" aria-hidden="true" />
            </span>
            <div>
              <h2 className="text-base font-semibold text-ink">
                Project structure
              </h2>
              <p className="text-xs text-muted">
                Clear boundaries for shared and feature code.
              </p>
            </div>
          </div>
          <ul className="mt-5 space-y-2">
            {structure.map(([path, purpose]) => (
              <li
                key={path}
                className="flex flex-col gap-0.5 rounded-lg px-2 py-1.5 text-sm transition-colors hover:bg-canvas sm:flex-row sm:items-center sm:justify-between sm:gap-4"
              >
                <code className="font-mono text-xs font-medium text-ink">
                  {path}
                </code>
                <span className="text-xs text-muted sm:text-right">
                  {purpose}
                </span>
              </li>
            ))}
          </ul>
        </Card>

        <Card className="p-6">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-xl bg-brand-soft text-brand">
              <Server className="size-4" aria-hidden="true" />
            </span>
            <div>
              <h2 className="text-base font-semibold text-ink">
                Runtime environment
              </h2>
              <p className="text-xs text-muted">
                Public values exposed through Vite.
              </p>
            </div>
          </div>
          <dl className="mt-5 space-y-2.5">
            <div className="flex items-center justify-between gap-4">
              <dt className="text-xs text-muted">Mode</dt>
              <dd className="text-sm font-medium text-ink">{env.mode}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-xs text-muted">Version</dt>
              <dd className="text-sm font-medium text-ink">
                {env.appVersion}
              </dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-xs text-muted">API base URL</dt>
              <dd className="break-all text-right font-mono text-xs font-medium text-ink">
                {env.apiBaseUrl || '/'}
              </dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-xs text-muted">Environment prefix</dt>
              <dd className="font-mono text-xs font-medium text-ink">VITE_</dd>
            </div>
          </dl>
          <p className="mt-4 rounded-xl bg-canvas/70 p-3 text-xs leading-5 text-muted">
            Copy <code className="font-mono text-ink">.env.example</code> to{' '}
            <code className="font-mono text-ink">.env.local</code> when local
            overrides are needed. Only variables prefixed with{' '}
            <code className="font-mono text-ink">VITE_</code> reach the browser.
          </p>
        </Card>

        <Card className="p-6">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-xl bg-brand-soft text-brand">
              <Terminal className="size-4" aria-hidden="true" />
            </span>
            <div>
              <h2 className="text-base font-semibold text-ink">
                Quality commands
              </h2>
              <p className="text-xs text-muted">
                Run from the frontend directory.
              </p>
            </div>
          </div>
          <ul className="mt-5 space-y-2">
            {commands.map(([command, description]) => (
              <li
                key={command}
                className="flex flex-col gap-1 rounded-lg border border-line/80 bg-canvas/50 px-3 py-2 sm:flex-row sm:items-center sm:justify-between"
              >
                <code className="font-mono text-xs font-semibold text-ink">
                  {command}
                </code>
                <span className="text-xs text-muted">{description}</span>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <Card tone="soft" className="p-6">
        <div className="grid gap-6 lg:grid-cols-[0.8fr_1.2fr] lg:items-start">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-brand">
              Module handoff
            </p>
            <h2 className="mt-2 text-xl font-semibold tracking-tight text-ink">
              Extend without rework
            </h2>
            <p className="mt-2 text-sm leading-6 text-muted">
              Feature work follows the same boundaries established here. Add
              capabilities deliberately and keep each module independently
              testable.
            </p>
          </div>
          <ol className="space-y-2.5">
            {handoffSteps.map((step, index) => (
              <li
                key={step}
                className="flex items-start gap-3 rounded-xl border border-brand/10 bg-surface/80 px-4 py-3 text-sm text-ink"
              >
                <span className="grid size-6 shrink-0 place-items-center rounded-full bg-brand-soft text-[11px] font-semibold text-brand">
                  {index + 1}
                </span>
                <span>{step}</span>
              </li>
            ))}
          </ol>
        </div>
      </Card>
    </div>
  )
}
