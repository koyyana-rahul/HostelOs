import {
  AlertCircle,
  Building2,
  CheckCircle2,
  LoaderCircle,
  LockKeyhole,
  LogIn,
  ShieldCheck,
} from 'lucide-react'
import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
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
  useLazyGetCurrentUserQuery,
  useLazyGetMembershipsQuery,
  useLoginMutation,
} from '../api/authApi.js'
import { sessionEstablished, selectAuthStatus } from '../authSlice.js'
import { getLoginErrorMessage } from '../lib/authErrors.js'
import { getLandingPath } from '../lib/roleResolver.js'

const inputClassName =
  'h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink transition-colors placeholder:text-muted/70 hover:border-brand/30 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted'

function isSafeDestination(path) {
  return (
    typeof path === 'string' &&
    path.startsWith('/') &&
    !path.startsWith('//') &&
    path !== '/login'
  )
}

function getDestination(state) {
  const from = state?.from

  if (typeof from === 'string') {
    return isSafeDestination(from) ? from : '/'
  }

  if (!from || typeof from.pathname !== 'string') {
    return '/'
  }

  const path = `${from.pathname}${from.search ?? ''}${from.hash ?? ''}`
  return isSafeDestination(path) ? path : '/'
}

export default function LoginPage() {
  useDocumentTitle('Sign in')

  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const status = useAppSelector(selectAuthStatus)
  const [getCsrfToken] = useLazyGetCsrfTokenQuery()
  const [loginRequest] = useLoginMutation()
  const [getCurrentUser] = useLazyGetCurrentUserQuery()
  const [getMemberships] = useLazyGetMembershipsQuery()
  const [loginIdentifier, setLoginIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const destination = getDestination(location.state)

  if (status === 'authenticated') {
    return <Navigate to={destination} replace />
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

    if (!normalizedIdentifier || !password) {
      setError('Enter both your login identifier and password.')
      return
    }

    if (normalizedIdentifier.length > 254) {
      setError('The login identifier is longer than 254 characters.')
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
      await loginRequest({
        loginIdentifier: normalizedIdentifier,
        password,
        csrf,
      }).unwrap()
      dispatch(authApi.util.resetApiState())
      const user = await getCurrentUser().unwrap()
      dispatch(sessionEstablished(user))
      let memberships = []
      try {
        memberships = await getMemberships().unwrap()
      } catch {
        memberships = []
      }
      setPassword('')
      navigate(getLandingPath(user, memberships), { replace: true })
    } catch (requestError) {
      setError(getLoginErrorMessage(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-dvh bg-canvas lg:grid lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]">
      <section className="relative hidden min-h-dvh overflow-hidden bg-brand p-10 text-white lg:flex lg:flex-col xl:p-14">
        <div
          className="surface-grid pointer-events-none absolute inset-0 opacity-40"
          aria-hidden="true"
        />
        <div className="relative z-10 flex h-full flex-col">
          <div className="flex items-center gap-3">
            <span className="grid size-11 place-items-center rounded-xl border border-white/15 bg-white/10">
              <Building2 className="size-5" aria-hidden="true" />
            </span>
            <div>
              <p className="text-base font-semibold tracking-tight">
                {env.appName}
              </p>
              <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-white/55">
                Operations workspace
              </p>
            </div>
          </div>

          <div className="my-auto max-w-md py-16">
            <span className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-white/75">
              <ShieldCheck className="size-3.5" aria-hidden="true" />
              Secure access
            </span>
            <h2 className="mt-6 text-4xl font-semibold leading-tight tracking-[-0.03em] xl:text-5xl">
              One workspace for hostel operations.
            </h2>
            <p className="mt-4 text-sm leading-6 text-white/70">
              Sign in with your HostelOS account to continue. Access is
              confirmed by the backend before any workspace data is shown.
            </p>
            <ul className="mt-8 space-y-3 text-sm text-white/85">
              <li className="flex items-center gap-3">
                <CheckCircle2
                  className="size-4 shrink-0 text-white/55"
                  aria-hidden="true"
                />
                Server-managed session with automatic expiry
              </li>
              <li className="flex items-center gap-3">
                <CheckCircle2
                  className="size-4 shrink-0 text-white/55"
                  aria-hidden="true"
                />
                Roles supplied by the backend, never selected in the browser
              </li>
              <li className="flex items-center gap-3">
                <CheckCircle2
                  className="size-4 shrink-0 text-white/55"
                  aria-hidden="true"
                />
                Protected routes backed by server-side authorization
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
              <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
                Welcome back
              </p>
              <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink">
                Sign in to HostelOS
              </h1>
              <p className="mt-2 text-sm leading-6 text-muted">
                Use the account supplied by your administrator.
              </p>

              <form
                className="mt-8 space-y-5"
                onSubmit={handleSubmit}
                noValidate
                aria-busy={isSubmitting}
              >
                {error ? (
                  <div
                    id="sign-in-error"
                    role="alert"
                    className="flex items-start gap-3 rounded-xl border border-danger/15 bg-danger-soft/60 p-4"
                  >
                    <AlertCircle
                      className="mt-0.5 size-4 shrink-0 text-danger"
                      aria-hidden="true"
                    />
                    <p className="text-sm leading-5 text-ink">{error}</p>
                  </div>
                ) : null}

                <div>
                  <label
                    htmlFor="loginIdentifier"
                    className="text-sm font-medium text-ink"
                  >
                    Email or username
                  </label>
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
                    aria-describedby="login-identifier-help"
                    aria-invalid={Boolean(error)}
                    className={`mt-2 ${inputClassName}`}
                  />
                  <p
                    id="login-identifier-help"
                    className="mt-1.5 text-xs leading-5 text-muted"
                  >
                    This is the login identifier assigned to your account.
                  </p>
                </div>

                <div>
                  <label
                    htmlFor="password"
                    className="text-sm font-medium text-ink"
                  >
                    Password
                  </label>
                  <input
                    id="password"
                    name="password"
                    type="password"
                    value={password}
                    onChange={updateField(setPassword)}
                    autoComplete="current-password"
                    disabled={isSubmitting}
                    maxLength={200}
                    aria-invalid={Boolean(error)}
                    className={`mt-2 ${inputClassName}`}
                  />
                </div>

                <Button
                  type="submit"
                  className="w-full"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <LoaderCircle
                      className="size-4 animate-spin"
                      aria-hidden="true"
                    />
                  ) : (
                    <LogIn className="size-4" aria-hidden="true" />
                  )}
                  {isSubmitting ? 'Signing in...' : 'Sign in'}
                </Button>
              </form>

              <div className="mt-6 flex items-start gap-2.5 border-t border-line/80 pt-5">
                <LockKeyhole
                  className="mt-0.5 size-3.5 shrink-0 text-muted"
                  aria-hidden="true"
                />
                <p className="text-xs leading-5 text-muted">
                  Your password is sent only to the HostelOS API. The session
                  cookie is HttpOnly, and credentials are never stored in
                  browser storage.
                </p>
              </div>
            </Card>

            <p className="mt-5 text-center text-sm text-muted">
              Don't have an account? <Link to="/register" className="text-brand hover:underline">Create account</Link>
            </p>
            <p className="mt-2 text-center text-xs text-muted">
              Access is verified against the HostelOS backend.
            </p>
          </div>
        </div>

        <footer className="px-5 py-5 text-center text-xs text-muted sm:px-8 lg:text-right">
          © {new Date().getFullYear()} {env.appName} · v{env.appVersion}
        </footer>
      </section>
    </main>
  )
}
