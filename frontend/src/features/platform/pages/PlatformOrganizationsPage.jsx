import { Building2, Search, RefreshCw } from 'lucide-react'
import { Card } from '../../../components/ui/Card.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { useListOrganizationsQuery } from '../../organizations/api/organizationApi.js'
import { useSuspendOrganizationMutation, useReactivateOrganizationMutation } from '../api/platformApi.js'
import { useState } from 'react'

export default function PlatformOrganizationsPage() {
  const { data: organizations = [], isLoading, refetch } = useListOrganizationsQuery()
  const [suspendOrg] = useSuspendOrganizationMutation()
  const [reactivateOrg] = useReactivateOrganizationMutation()
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [pendingId, setPendingId] = useState(null)

  const filtered = organizations.filter((org) => {
    const matchesSearch =
      !search ||
      org.name.toLowerCase().includes(search.toLowerCase()) ||
      org.slug.toLowerCase().includes(search.toLowerCase())
    const matchesStatus = statusFilter === 'ALL' || org.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const handleSuspend = async (id) => {
    if (!confirm('Suspend this organization?')) return
    setPendingId(id)
    try {
      await suspendOrg(id).unwrap()
    } catch {
      alert('Failed to suspend organization')
    } finally {
      setPendingId(null)
    }
  }

  const handleReactivate = async (id) => {
    if (!confirm('Reactivate this organization?')) return
    setPendingId(id)
    try {
      await reactivateOrg(id).unwrap()
    } catch {
      alert('Failed to reactivate organization')
    } finally {
      setPendingId(null)
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">Organizations</h1>
          <p className="mt-1 text-sm text-muted">Manage all organizations</p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => refetch()} disabled={isLoading}>
          <RefreshCw className={`size-4 ${isLoading ? 'animate-spin' : ''}`} aria-hidden="true" />
          Refresh
        </Button>
      </header>

      <Card className="p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" aria-hidden="true" />
            <input
              type="text"
              placeholder="Search organizations..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="h-10 w-full rounded-lg border border-line bg-surface pl-9 pr-3 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-10 rounded-lg border border-line bg-surface px-3 text-sm text-ink"
          >
            <option value="ALL">All Status</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="SUSPENDED">SUSPENDED</option>
            <option value="PENDING">PENDING</option>
            <option value="DISABLED">DISABLED</option>
          </select>
        </div>
      </Card>

      <Card className="overflow-hidden">
        <div className="border-b border-line px-6 py-4">
          <h2 className="text-sm font-semibold text-ink">Organizations ({filtered.length})</h2>
        </div>
        <div className="divide-y divide-line">
          {isLoading ? (
            <div className="px-6 py-8 text-center text-sm text-muted">Loading...</div>
          ) : filtered.length === 0 ? (
            <div className="px-6 py-8 text-center text-sm text-muted">No organizations found</div>
          ) : (
            filtered.map((org) => (
              <div key={org.id} className="flex items-center gap-3 px-6 py-4">
                <span className="grid size-9 place-items-center rounded-lg bg-brand-soft">
                  <Building2 className="size-4 text-brand" aria-hidden="true" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-ink">{org.name}</p>
                  <p className="text-xs text-muted">
                    {org.slug} • Created {org.createdAt ? new Date(org.createdAt).toLocaleDateString() : '—'}
                  </p>
                </div>
                <span className="rounded-full px-2 py-1 text-xs font-medium border border-line">{org.status}</span>
                <div className="flex items-center gap-2">
                  {org.status === 'ACTIVE' ? (
                    <Button size="sm" variant="secondary" onClick={() => handleSuspend(org.id)} disabled={pendingId === org.id}>
                      Suspend
                    </Button>
                  ) : org.status === 'SUSPENDED' ? (
                    <Button size="sm" variant="secondary" onClick={() => handleReactivate(org.id)} disabled={pendingId === org.id}>
                      Reactivate
                    </Button>
                  ) : null}
                </div>
              </div>
            ))
          )}
        </div>
      </Card>
    </div>
  )
}
