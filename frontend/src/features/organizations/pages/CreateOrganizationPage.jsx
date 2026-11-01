import { AlertCircle, LoaderCircle, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Brand } from '../../../components/layout/Brand.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { organizationApi, useCreateOrganizationMutation } from '../api/organizationApi.js'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { setCurrentOrganization } from '../organizationSlice.js'

const inputClassName =
  'h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink transition-colors placeholder:text-muted/70 hover:border-brand/30 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted'

function slugify(value) {
  return value
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/-{2,}/g, '-')
}

export default function CreateOrganizationPage() {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const [createOrganization, { isLoading }] = useCreateOrganizationMutation()
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [slugEdited, setSlugEdited] = useState(false)
  const [error, setError] = useState(null)

  const updateName = (event) => {
    const value = event.target.value
    setName(value)
    if (!slugEdited) {
      setSlug(slugify(value))
    }
    if (error) setError(null)
  }

  const updateSlug = (event) => {
    setSlugEdited(true)
    setSlug(slugify(event.target.value))
    if (error) setError(null)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (isLoading) return
    setError(null)

    const trimmedName = name.trim()
    const trimmedSlug = slug.trim()

    if (!trimmedName) {
      setError('Enter an organization name.')
      return
    }
    if (trimmedName.length > 120) {
      setError('Organization name must not exceed 120 characters.')
      return
    }
    if (trimmedSlug.length < 3) {
      setError('Organization slug must be at least 3 characters.')
      return
    }
    if (trimmedSlug.length > 60) {
      setError('Organization slug must not exceed 60 characters.')
      return
    }
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(trimmedSlug)) {
      setError('Slug must consist of lowercase letters, numbers, and hyphens only.')
      return
    }

    try {
      const result = await createOrganization({ name: trimmedName, slug: trimmedSlug }).unwrap()
      dispatch(
        organizationApi.util.invalidateTags([
          'Membership',
          { type: 'Organization', id: 'LIST' },
          { type: 'Organization', id: 'MINE' },
        ]),
      )
      if (result?.id) {
        dispatch(setCurrentOrganization(result.id))
      }
      navigate('/app/overview', { replace: true })
    } catch (err) {
      if (err?.status === 409) {
        setError('An organization with this slug already exists.')
      } else if (err?.status === 400 && err?.data?.message) {
        setError(err.data.message)
      } else {
        setError('Failed to create organization. Try again.')
      }
    }
  }

  return (
    <main className="min-h-dvh bg-canvas">
      <header className="border-b border-line/70">
        <div className="mx-auto flex h-16 w-full max-w-4xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Brand />
          <Link to="/onboarding" className="text-sm text-muted hover:text-ink">
            Back to onboarding
          </Link>
        </div>
      </header>

      <div className="mx-auto w-full max-w-2xl px-4 py-10 sm:px-6">
        <div className="mb-8">
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">Create organization</p>
          <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink">New organization</h1>
          <p className="mt-2 text-sm leading-6 text-muted">
            Create an organization to manage properties, listings, enquiries, and
            your public presence.
          </p>
        </div>

        <Card className="p-6 sm:p-8">
          <form onSubmit={handleSubmit} noValidate>
            {error ? (
              <div role="alert" className="mb-6 flex items-start gap-3 rounded-xl border border-danger/15 bg-danger-soft/60 p-4">
                <AlertCircle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden="true" />
                <p className="text-sm leading-5 text-ink">{error}</p>
              </div>
            ) : null}

            <div className="space-y-5">
              <div>
                <label htmlFor="org-name" className="text-sm font-medium text-ink">
                  Organization name
                </label>
                <input
                  id="org-name"
                  type="text"
                  value={name}
                  onChange={updateName}
                  maxLength={120}
                  className={`mt-2 ${inputClassName}`}
                  placeholder="Sunrise Hostel Group"
                />
              </div>

              <div>
                <label htmlFor="org-slug" className="text-sm font-medium text-ink">
                  URL slug
                </label>
                <input
                  id="org-slug"
                  type="text"
                  value={slug}
                  onChange={updateSlug}
                  maxLength={60}
                  className={`mt-2 ${inputClassName}`}
                  placeholder="sunrise-hostel"
                />
                <p className="mt-2 text-xs text-muted">
                  Lowercase letters, numbers, and hyphens. Used in URLs.
                </p>
              </div>
            </div>

            <div className="mt-8 flex items-center gap-3">
              <Button type="submit" disabled={isLoading}>
                {isLoading ? (
                  <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                ) : (
                  <Plus className="size-4" aria-hidden="true" />
                )}
                {isLoading ? 'Creating...' : 'Create organization'}
              </Button>
              <Button variant="secondary" as={Link} to="/onboarding">
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </main>
  )
}
