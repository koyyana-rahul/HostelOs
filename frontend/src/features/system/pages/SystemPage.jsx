import {
  AlertTriangle,
  LoaderCircle,
  RefreshCw,
  Server,
  ShieldCheck,
} from 'lucide-react'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { env } from '../../../lib/env.js'
import { cn } from '../../../utils/cn.js'
import { useGetSystemInfoQuery } from '../api/systemApi.js'
import {
  getStatusTone,
  getSystemInfoErrorMessage,
  validateSystemInfo,
} from '../lib/systemInfo.js'

function LoadingState() {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-center gap-3 rounded-xl border border-line bg-canvas/50 p-4"
    >
      <LoaderCircle
        className="size-5 shrink-0 animate-spin text-brand"
        aria-hidden="true"
      />
      <div>
        <p className="text-sm font-semibold text-ink">Contacting the backend</p>
        <p className="mt-0.5 text-xs leading-5 text-muted">
          Requesting /api/v1/system/info from the configured API base.
        </p>
      </div>
    </div>
  )
}

function ErrorPanel({ message, onRetry, isRetrying }) {
  return (
    <div
      role="alert"
      className="rounded-xl border border-danger/15 bg-danger-soft/50 p-5"
    >
      <div className="flex items-start gap-3">
        <AlertTriangle
          className="mt-0.5 size-5 shrink-0 text-danger"
          aria-hidden="true"
        />
        <div>
          <h2 className="text-sm font-semibold text-ink">
            System information unavailable
          </h2>
          <p className="mt-1 text-sm leading-6 text-muted">{message}</p>
          <Button
            variant="secondary"
            className="mt-4 whitespace-nowrap"
            onClick={onRetry}
            disabled={isRetrying}
          >
            <RefreshCw
              className={cn('size-4', isRetrying && 'animate-spin')}
              aria-hidden="true"
            />
            {isRetrying ? 'Retrying...' : 'Retry request'}
          </Button>
        </div>
      </div>
    </div>
  )
}

function SuccessState({ data, onRefresh, isFetching }) {
  return (
    <div aria-live="polite">
      <dl className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-xl border border-line bg-canvas/50 p-4">
          <dt className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
            Application name
          </dt>
          <dd className="mt-1.5 break-words text-sm font-semibold text-ink">
            {data.applicationName}
          </dd>
        </div>
        <div className="rounded-xl border border-line bg-canvas/50 p-4">
          <dt className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
            API version
          </dt>
          <dd className="mt-1.5 text-sm font-semibold text-ink">
            {data.apiVersion}
          </dd>
        </div>
        <div className="rounded-xl border border-line bg-canvas/50 p-4">
          <dt className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
            Endpoint status
          </dt>
          <dd className="mt-1.5">
            <Badge tone={getStatusTone(data.status)} dot>
              {data.status}
            </Badge>
          </dd>
        </div>
      </dl>

      <div className="mt-5 flex flex-col gap-3 rounded-xl border border-line bg-canvas/50 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
            Verified response
          </p>
          <p className="mt-1 text-xs leading-5 text-muted">
            The metadata endpoint returned the values above. This does not
            confirm platform-wide health.
          </p>
        </div>
        <Button
          variant="secondary"
          className="shrink-0 whitespace-nowrap"
          onClick={onRefresh}
          disabled={isFetching}
        >
          <RefreshCw
            className={cn('size-4', isFetching && 'animate-spin')}
            aria-hidden="true"
          />
          {isFetching ? 'Checking...' : 'Check again'}
        </Button>
      </div>
    </div>
  )
}

export default function SystemPage() {
  useDocumentTitle('System')
  const { data, error, isLoading, isFetching, refetch } =
    useGetSystemInfoQuery()

  const validation = data ? validateSystemInfo(data) : null
  const hasValidData = validation?.valid === true
  const hasError = Boolean(error) || Boolean(validation && !validation.valid)
  const message = getSystemInfoErrorMessage(error, validation)

  const retry = () => {
    refetch().catch(() => undefined)
  }

  return (
    <div className="space-y-6">
      <header>
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
          Live API integration
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">
          System information
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">
          Read-only metadata requested from the HostelOS backend. This check
          covers the system-information endpoint only, not the whole platform.
        </p>
      </header>

      <div className="grid gap-4 lg:grid-cols-[1.25fr_0.75fr]">
        <Card className="p-6">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-xl bg-brand-soft text-brand">
              <Server className="size-4" aria-hidden="true" />
            </span>
            <div>
              <h2 className="text-base font-semibold text-ink">
                Backend metadata
              </h2>
              <p className="text-xs text-muted">
                GET /api/v1/system/info
              </p>
            </div>
          </div>

          <div className="mt-6">
            {isLoading ? <LoadingState /> : null}
            {!isLoading && hasError ? (
              <ErrorPanel
                message={message}
                onRetry={retry}
                isRetrying={isFetching}
              />
            ) : null}
            {!isLoading && !hasError && hasValidData ? (
              <SuccessState
                data={data}
                onRefresh={retry}
                isFetching={isFetching}
              />
            ) : null}
          </div>
        </Card>

        <Card tone="soft" className="p-6">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-xl bg-surface/80 text-brand">
              <ShieldCheck className="size-4" aria-hidden="true" />
            </span>
            <div>
              <h2 className="text-base font-semibold text-ink">
                What this verifies
              </h2>
              <p className="text-xs text-muted">
                Scope of the metadata check.
              </p>
            </div>
          </div>
          <ul className="mt-5 space-y-2.5 text-sm leading-6 text-ink">
            <li>
              RTK Query requests the configured backend using the shared API
              base URL.
            </li>
            <li>
              The page checks that the response contains applicationName,
              apiVersion, and status.
            </li>
            <li>
              This check does not confirm database health or business modules.
            </li>
          </ul>
          <div className="mt-5 rounded-xl border border-brand/10 bg-surface/80 p-4">
            <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
              API base
            </p>
            <p className="mt-1 break-all font-mono text-xs font-medium text-ink">
              {env.apiBaseUrl || '/'}
            </p>
          </div>
        </Card>
      </div>
    </div>
  )
}
