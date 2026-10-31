import {
  AlertCircle,
  Building2,
  CheckCircle2,
  LoaderCircle,
  LockKeyhole,
  UserPlus,
} from 'lucide-react'
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Brand } from '../../../components/layout/Brand.jsx'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { env } from '../../../lib/env.js'
import {
  authApi,
  useLazyGetCsrfTokenQuery,
  useRegisterMutation,
} from '../api/authApi.js'
import { selectAuthStatus } from '../authSlice.js'
import { getLoginErrorMessage } from '../lib/authErrors.js'

const inputClassName =
  'h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink transition-colors placeholder:text-muted/70 hover:border-brand/30 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted'

function getRegisterErrorMessage(error) {
  if (!error) {
    return 'Registration failed. Try again.'
  }
  if (error.status === 409) {
    return 'An account with this login identifier already exists.'
  }
  if (error.status === 400) {
    if (error.data?.message) {
      return error.data.message
    }
    return 'Please check the registration details and try again.'
  }
  return getLoginErrorMessage(error)
}

export default function RegisterPage() {
  useDocumentTitle('Create account')

  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const status = useAppSelector(selectAuthStatus)
  const [getCsrfToken] = useLazyGetCsrfTokenQuery()
  const [registerRequest] = useRegisterMutation()
  const [loginIdentifier, setLoginIdentifier] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  const updateField = (setter) => (event) => {
    setter(event.target.value)
    if (error) {
      setError(null)
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (isSubmitting) {
      return
    }

    const normalizedIdentifier = loginIdentifier.trim()
    const normalizedDisplayName = displayName.trim()

    if (!normalizedIdentifier || !normalizedDisplayName || !password) {
      setError('Enter login identifier, display name, and password.')
      return
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (normalizedIdentifier.length > 254) {
      setError('The login identifier is longer than 254 characters.')
      return
    }
    if (normalizedDisplayName.length > 254) {
      setError('The display name is longer than 254 characters.')
      return
    }
    if (password.length > 200) {
      setError('The password is longer than 200 characters.')
      return
    }

    setError(null)
    setIsSubmitting(true)

    try {
      const csrf = await getCsrfToken(undefined, false).unwrap()
      await registerRequest({
        loginIdentifier: normalizedIdentifier,
        displayName: normalizedDisplayName,
        password,
        csrf,
      }).unwrap()

      dispatch(authApi.util.resetApiState())
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (requestError) {
      setError(getRegisterErrorMessage(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-dvh bg-canvas lg:grid lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]">
      <section className="relative hidden min-h-dvh overflow-hidden bg-brand p-10 text-white lg:flex lg:flex-col xl:p-14">
        <div className="surface-grid pointer-events-none absolute inset-0 opacity-40" aria-hidden="true" />
        <div className="relative z-10 flex h-full flex-col">
          <div className="flex items-center gap-3">
            <span className="grid size-11 place-items-center rounded-xl border border-white/15 bg-white/10">
              <Building2 className="size-5" aria-hidden="true" />
            </span>
            <div>
              <p className="text-base font-semibold tracking-tight">{env.appName}</p>
              <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-white/55">Operations workspace</p>
            </div>
          </div>

          <div className="my-auto max-w-md py-16">
            <h2 className="mt-6 text-4xl font-semibold leading-tight tracking-[-0.03em] xl:text-5xl">
              Create your HostelOS account
            </h2>
            <p className="mt-4 text-sm leading-6 text-white/70">
              Register as a customer to access the HostelOS platform.
            </p>
            <ul className="mt-8 space-y-3 text-sm text-white/85">
              <li className="flex items-center gap-3">
                <CheckCircle2 className="size-4 shrink-0 text-white/55" aria-hidden="true" />
                Session-based authentication
              </li>
              <li className="flex items-center gap-3">
                <CheckCircle2 className="size-4 shrink-0 text-white/55" aria-hidden="true" />
                No automatic organization creation
              </li>
              <li className="flex items-center gap-3">
                <CheckCircle2 className="size-4 shrink-0 text-white/55" aria-hidden="true" />
                Secure CSRF-protected requests
              </li>
            </ul>
          </div>

          <p className="text-xs text-white/45">
            HostelOS · {env.environmentLabel} · v{env.appVersion}
          </p>
        </div>
      </section>

      <section className="flex min-h-dvh flex-col">
        <header className="flex items-center justify-between gap-4 px-5 py-5 sm:px-8 lg:hidden">
          <Brand />
          <Badge tone="brand">{env.environmentLabel}</Badge>
        </header>

        <div className="flex flex-1 items-center justify-center px-4 py-8 sm:px-8">
          <div className="w-full max-w-md">
            <Card className="p-6 sm:p-8">
              <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">Get started</p>
              <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink">Create account</h1>
              <p className="mt-2 text-sm leading-6 text-muted">Register for a customer account.</p>

              <form className="mt-8 space-y-5" onSubmit={handleSubmit} noValidate aria-busy={isSubmitting}>
                {error ? (
                  <div role="alert" className="flex items-start gap-3 rounded-xl border border-danger/15 bg-danger-soft/60 p-4">
                    <AlertCircle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden="true" />
                    <p className="text-sm leading-5 text-ink">{error}</p>
                  </div>
                ) : null}

                <div>
                  <label htmlFor="loginIdentifier" className="text-sm font-medium text-ink">Login identifier</label>
                  <input
                    id="loginIdentifier"
                    name="loginIdentifier"
                    type="text"
                    value={loginIdentifier}
                    onChange={updateField(setLoginIdentifier)}
                    autoComplete="username"
                    autoCapitalize="none"
                    spellCheck={false}
                    autoFocus
                    disabled={isSubmitting}
                    maxLength={254}
                    className={`mt-2 ${inputClassName}`}
                  />
                </div>

                <div>
                  <label htmlFor="displayName" className="text-sm font-medium text-ink">Display name</label>
                  <input
                    id="displayName"
                    name="displayName"
                    type="text"
                    value={displayName}
                    onChange={updateField(setDisplayName)}
                    autoComplete="name"
                    disabled={isSubmitting}
                    maxLength={254}
                    className={`mt-2 ${inputClassName}`}
                  />
                </div>

                <div>
                  <label htmlFor="password" className="text-sm font-medium text-ink">Password</label>
                  <input
                    id="password"
                    name="password"
                    type="password"
                    value={password}
                    onChange={updateField(setPassword)}
                    autoComplete="new-password"
                    disabled={isSubmitting}
                    maxLength={200}
                    className={`mt-2 ${inputClassName}`}
                  />
                </div>

                <div>
                  <label htmlFor="confirmPassword" className="text-sm font-medium text-ink">Confirm password</label>
                  <input
                    id="confirmPassword"
                    name="confirmPassword"
                    type="password"
                    value={confirmPassword}
                    onChange={updateField(setConfirmPassword)}
                    autoComplete="new-password"
                    disabled={isSubmitting}
                    maxLength={200}
                    className={`mt-2 ${inputClassName}`}
                  />
                </div>

                <Button type="submit" className="w-full" disabled={isSubmitting}>
                  {isSubmitting ? (
                    <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                  ) : (
                    <UserPlus className="size-4" aria-hidden="true" />
                  )}
                  {isSubmitting ? 'Creating account...' : 'Create account'}
                </Button>
              </form>

              <div className="mt-6 flex items-start gap-2.5 border-t border-line/80 pt-5">
                <LockKeyhole className="mt-0.5 size-3.5 shrink-0 text-muted" aria-hidden="true" />
                <p className="text-xs leading-5 text-muted">
                  CSRF protected. Session-based authentication.
                </p>
              </div>

              <p className="mt-4 text-center text-sm text-muted">
                Already have an account? <Link to="/login" className="text-brand hover:underline">Sign in</Link>
              </p>
            </Card>
          </div>
        </div>

        <footer className="px-5 py-5 text-center text-xs text-muted sm:px-8 lg:text-right">
          © {new Date().getFullYear()} {env.appName} · v{env.appVersion}
        </footer>
      </section>
    </main>
  )
}
