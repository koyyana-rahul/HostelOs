import { UsersRound, Search, RefreshCw } from 'lucide-react'
import { Card } from '../../../components/ui/Card.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { useGetPlatformUsersQuery } from '../api/platformApi.js'
import { useState } from 'react'

export default function PlatformUsersPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const { data, isLoading, refetch } = useGetPlatformUsersQuery({ search, page, size: 20 })

  const users = data?.content || data || []
  const totalPages = data?.totalPages || 1

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">Users</h1>
          <p className="mt-1 text-sm text-muted">Platform user management</p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => refetch()} disabled={isLoading}>
          <RefreshCw className={`size-4 ${isLoading ? 'animate-spin' : ''}`} aria-hidden="true" />
          Refresh
        </Button>
      </header>

      <Card className="p-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" aria-hidden="true" />
          <input
            type="text"
            placeholder="Search by login identifier or display name..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(0)
            }}
            className="h-10 w-full rounded-lg border border-line bg-surface pl-9 pr-3 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15"
          />
        </div>
      </Card>

      <Card className="overflow-hidden">
        <div className="border-b border-line px-6 py-4">
          <h2 className="text-sm font-semibold text-ink">Users</h2>
        </div>
        <div className="divide-y divide-line">
          {isLoading ? (
            <div className="px-6 py-8 text-center text-sm text-muted">Loading...</div>
          ) : users.length === 0 ? (
            <div className="px-6 py-8 text-center text-sm text-muted">No users found</div>
          ) : (
            users.map((user) => (
              <div key={user.id} className="flex items-center gap-3 px-6 py-4">
                <span className="grid size-9 place-items-center rounded-lg bg-brand-soft">
                  <UsersRound className="size-4 text-brand" aria-hidden="true" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-ink">{user.displayName}</p>
                  <p className="text-xs text-muted">{user.loginIdentifier}</p>
                </div>
                <div className="flex flex-wrap gap-1">
                  {(user.roles || []).map((role) => (
                    <span key={role} className="rounded-full px-2 py-1 text-xs border border-line">
                      {role}
                    </span>
                  ))}
                </div>
                <span className="text-xs text-muted">{user.status}</span>
              </div>
            ))
          )}
        </div>
        {totalPages > 1 ? (
          <div className="flex items-center justify-between border-t border-line px-6 py-3">
            <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Previous
            </Button>
            <span className="text-xs text-muted">
              Page {page + 1} of {totalPages}
            </span>
            <Button size="sm" variant="secondary" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
              Next
            </Button>
          </div>
        ) : null}
      </Card>
    </div>
  )
}
