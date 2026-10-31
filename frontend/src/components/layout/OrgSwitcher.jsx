import { useGetMembershipsQuery } from '../../features/auth/api/authApi.js'
import { selectCurrentUser } from '../../features/auth/authSlice.js'
import { useAppSelector } from '../../hooks/useAppSelector.js'
import { useAppDispatch } from '../../hooks/useAppDispatch.js'
import { setCurrentOrganization } from '../../features/organizations/organizationSlice.js'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'

export function OrgSwitcher() {
  const user = useAppSelector(selectCurrentUser)
  const { data: memberships = [] } = useGetMembershipsQuery()
  const dispatch = useAppDispatch()
  const active = memberships.filter((m) => m.status === 'ACTIVE')

  if (!user || active.length === 0) return null

  return (
    <div className="border-b border-line/70 px-4 py-3 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted">Organization</span>
          <select
            className="h-8 rounded-lg border border-line bg-surface px-2 text-xs text-ink"
            onChange={(e) => dispatch(setCurrentOrganization(Number(e.target.value)))}
            defaultValue={active[0]?.organizationId}
          >
            {active.map((m) => (
              <option key={m.id} value={m.organizationId}>
                {m.organizationName} ({m.orgRole})
              </option>
            ))}
          </select>
        </div>
        <Link to="/onboarding/create" className="flex items-center gap-1 text-xs text-brand hover:underline">
          <Plus className="size-3" aria-hidden="true" />
          New organization
        </Link>
      </div>
    </div>
  )
}
