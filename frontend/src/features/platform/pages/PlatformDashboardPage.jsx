import { Building2, UsersRound, Activity, RefreshCw } from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { useGetPlatformStatsQuery } from '../api/platformApi.js'

function formatDate(value) {
  if (!value) return '—'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}

export default function PlatformDashboardPage() {
  const { data, isLoading, isError, refetch } = useGetPlatformStatsQuery()

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">Platform Dashboard</h1>
          <p className="mt-1 text-sm text-muted">Platform administration overview</p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => refetch()} disabled={isLoading}>
          <RefreshCw className={`size-4 ${isLoading ? 'animate-spin' : ''}`} aria-hidden="true" />
          Refresh
        </Button>
      </header>

      {isError ? (
        <div className="rounded-xl border border-danger/15 bg-danger-soft/60 p-4 text-sm text-ink">
          Failed to load platform statistics.
        </div>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-line p-5">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted">Total Organizations</p>
            <Building2 className="size-4 text-muted" aria-hidden="true" />
          </div>
          <p className="mt-2 text-2xl font-semibold text-ink">{isLoading ? '—' : data?.totalOrganizations ?? 0}</p>
        </div>
        <div className="rounded-2xl border border-line p-5">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted">Active Organizations</p>
            <Building2 className="size-4 text-muted" aria-hidden="true" />
          </div>
          <p className="mt-2 text-2xl font-semibold text-ink">{isLoading ? '—' : data?.activeOrganizations ?? 0}</p>
        </div>
        <div className="rounded-2xl border border-line p-5">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted">Total Users</p>
            <UsersRound className="size-4 text-muted" aria-hidden="true" />
          </div>
          <p className="mt-2 text-2xl font-semibold text-ink">{isLoading ? '—' : data?.totalUsers ?? 0}</p>
        </div>
        <div className="rounded-2xl border border-line p-5">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted">Active Users</p>
            <UsersRound className="size-4 text-muted" aria-hidden="true" />
          </div>
          <p className="mt-2 text-2xl font-semibold text-ink">{isLoading ? '—' : data?.activeUsers ?? 0}</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-line">
          <div className="border-b border-line px-6 py-4">
            <h2 className="text-sm font-semibold text-ink">Recent Organizations</h2>
          </div>
          <div className="divide-y divide-line">
            {isLoading ? (
              <div className="px-6 py-6 text-center text-sm text-muted">Loading...</div>
            ) : data?.recentOrganizations?.length ? (
              data.recentOrganizations.map((org) => (
                <div key={org.id} className="flex items-center justify-between px-6 py-4">
                  <div>
                    <p className="font-medium text-ink">{org.name}</p>
                    <p className="text-xs text-muted">{org.slug}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-muted">{org.status}</p>
                    <p className="text-xs text-muted">{formatDate(org.createdAt)}</p>
                  </div>
                </div>
              ))
            ) : (
              <div className="px-6 py-6 text-center text-sm text-muted">No organizations yet</div>
            )}
          </div>
        </div>

        <div className="rounded-2xl border border-line">
          <div className="border-b border-line px-6 py-4">
            <h2 className="text-sm font-semibold text-ink">Recent Users</h2>
          </div>
          <div className="divide-y divide-line">
            {isLoading ? (
              <div className="px-6 py-6 text-center text-sm text-muted">Loading...</div>
            ) : data?.recentUsers?.length ? (
              data.recentUsers.map((user) => (
                <div key={user.id} className="flex items-center justify-between px-6 py-4">
                  <div>
                    <p className="font-medium text-ink">{user.displayName}</p>
                    <p className="text-xs text-muted">{user.loginIdentifier}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-muted">{user.status}</p>
                    <p className="text-xs text-muted">{formatDate(user.createdAt)}</p>
                  </div>
                </div>
              ))
            ) : (
              <div className="px-6 py-6 text-center text-sm text-muted">No users yet</div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
