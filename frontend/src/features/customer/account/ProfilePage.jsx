import { useState } from 'react'
import { LoaderCircle } from 'lucide-react'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { sessionEstablished } from '../../auth/authSlice.js'
import {
  useGetMyProfileQuery,
  useUpdateMyProfileMutation,
} from '../api/customerApi.js'

export default function AccountProfilePage() {
  useDocumentTitle('My profile')
  const dispatch = useAppDispatch()
  const { data: profile, isLoading, isError, error, refetch } =
    useGetMyProfileQuery()
  const [updateProfile, updateState] = useUpdateMyProfileMutation()
  const [displayName, setDisplayName] = useState(null)
  const [message, setMessage] = useState('')

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMessage('')
    const nextDisplayName = displayName ?? profile?.displayName ?? ''
    if (!nextDisplayName.trim()) {
      setMessage('Display name must not be blank.')
      return
    }

    try {
      const updated = await updateProfile({ displayName: nextDisplayName }).unwrap()
      dispatch(sessionEstablished(updated))
      setMessage('Profile updated.')
    } catch (requestError) {
      setMessage(
        requestError?.data?.message ?? 'Could not update your profile.',
      )
    }
  }

  if (isLoading) {
    return (
      <div className="flex h-40 items-center justify-center">
        <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
      </div>
    )
  }

  if (isError) {
    return (
      <ErrorState
        title="Could not load your profile"
        description={error?.data?.message ?? 'Try again in a moment.'}
        action={
          <Button onClick={() => refetch()} variant="secondary">
            Retry
          </Button>
        }
      />
    )
  }

  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-xl font-semibold tracking-[-0.02em] text-ink">
          My profile
        </h2>
        <p className="mt-1 text-sm text-muted">
          Update your customer profile without changing your account access.
        </p>
      </header>

      <Card className="p-6">
        <form className="space-y-4" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="profile-display-name" className="text-sm font-medium text-ink">
              Display name
            </label>
            <input
              id="profile-display-name"
              value={displayName ?? profile?.displayName ?? ''}
              onChange={(event) => setDisplayName(event.target.value)}
              maxLength={120}
              required
              disabled={updateState.isLoading}
              className="mt-2 h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15"
            />
          </div>
          <div className="border-t border-line/70 pt-3 text-sm text-muted">
            Login identifier: <span className="font-medium text-ink">{profile?.loginIdentifier}</span>
          </div>
          {message ? (
            <p role={message === 'Profile updated.' ? 'status' : 'alert'} className="text-sm text-muted">
              {message}
            </p>
          ) : null}
          <Button type="submit" disabled={updateState.isLoading}>
            {updateState.isLoading ? 'Saving...' : 'Save profile'}
          </Button>
        </form>
      </Card>

      <Card className="p-6">
        <h3 className="text-sm font-semibold text-ink">Roles</h3>
        <ul className="mt-3 flex flex-wrap gap-2">
          {(profile?.roles || []).map((role) => (
            <li key={role}>
              <Badge tone="brand">{role}</Badge>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  )
}
