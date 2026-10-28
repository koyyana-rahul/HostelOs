import { Menu, X } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import {
  closeMobileNav,
  openMobileNav,
  selectIsMobileNavOpen,
} from '../../app/uiSlice.js'
import { useAppDispatch } from '../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../hooks/useAppSelector.js'
import { env } from '../../lib/env.js'
import { Badge } from '../ui/Badge.jsx'
import { SidebarContent } from './SidebarContent.jsx'

export function AppLayout() {
  const dispatch = useAppDispatch()
  const isMobileNavOpen = useAppSelector(selectIsMobileNavOpen)
  const location = useLocation()
  const menuButtonRef = useRef(null)
  const closeButtonRef = useRef(null)
  const wasMobileNavOpen = useRef(false)

  useEffect(() => {
    dispatch(closeMobileNav())
  }, [location.pathname, dispatch])

  useEffect(() => {
    if (isMobileNavOpen) {
      wasMobileNavOpen.current = true
      closeButtonRef.current?.focus()
    } else if (wasMobileNavOpen.current) {
      wasMobileNavOpen.current = false
      menuButtonRef.current?.focus()
    }
  }, [isMobileNavOpen])

  useEffect(() => {
    if (!isMobileNavOpen) {
      return undefined
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        dispatch(closeMobileNav())
      }
    }

    const previousOverflow = document.body.style.overflow
    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [isMobileNavOpen, dispatch])

  return (
    <div className="min-h-dvh bg-canvas lg:grid lg:grid-cols-[272px_minmax(0,1fr)]">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-white"
      >
        Skip to content
      </a>

      <aside className="hidden border-r border-line/80 bg-surface lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col">
        <SidebarContent />
      </aside>

      {isMobileNavOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            aria-label="Dismiss navigation"
            tabIndex={-1}
            onClick={() => dispatch(closeMobileNav())}
            className="absolute inset-0 bg-ink/35 backdrop-blur-[2px]"
          />
          <div
            id="mobile-navigation"
            role="dialog"
            aria-modal="true"
            aria-label="Application navigation"
            className="relative flex h-full w-[min(20rem,86vw)] flex-col border-r border-line bg-surface shadow-2xl"
          >
            <div className="absolute right-3 top-3 z-10">
              <button
                ref={closeButtonRef}
                type="button"
                aria-label="Close navigation"
                onClick={() => dispatch(closeMobileNav())}
                className="grid size-9 place-items-center rounded-lg text-muted transition-colors hover:bg-canvas hover:text-ink"
              >
                <X className="size-4" aria-hidden="true" />
              </button>
            </div>
            <SidebarContent
              onNavigate={() => dispatch(closeMobileNav())}
            />
          </div>
        </div>
      ) : null}

      <div className="flex min-h-dvh min-w-0 flex-col">
        <header className="sticky top-0 z-30 border-b border-line/70 bg-canvas/85 backdrop-blur-md">
          <div className="flex h-16 items-center gap-3 px-4 sm:px-6 lg:px-8">
            <button
              ref={menuButtonRef}
              type="button"
              aria-label="Open navigation"
              aria-expanded={isMobileNavOpen}
              aria-controls={
                isMobileNavOpen ? 'mobile-navigation' : undefined
              }
              onClick={() => dispatch(openMobileNav())}
              className="grid size-9 shrink-0 place-items-center rounded-lg border border-line bg-surface text-ink transition-colors hover:bg-brand-soft lg:hidden"
            >
              <Menu className="size-4" aria-hidden="true" />
            </button>

            <div className="min-w-0">
              <p className="hidden text-[10px] font-semibold uppercase tracking-[0.2em] text-muted sm:block">
                Operations workspace
              </p>
              <p className="truncate text-sm font-medium text-ink">
                Hostel management console
              </p>
            </div>

            <div className="ml-auto flex items-center gap-2">
              <div className="hidden sm:block">
                <Badge tone="success" dot>
                  {env.environmentLabel}
                </Badge>
              </div>
              <span className="hidden rounded-full border border-line bg-surface px-2.5 py-1 text-xs font-medium text-muted sm:inline-flex">
                v{env.appVersion}
              </span>
            </div>
          </div>
        </header>

        <main id="main-content" tabIndex={-1} className="flex-1">
          <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
            <Outlet />
          </div>
        </main>

        <footer className="border-t border-line/70">
          <div className="mx-auto flex w-full max-w-6xl flex-col gap-2 px-4 py-5 text-xs text-muted sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
            <p>
              © {new Date().getFullYear()} {env.appName}. Frontend foundation.
            </p>
            <p>React · Vite · Tailwind CSS · RTK Query</p>
          </div>
        </footer>
      </div>
    </div>
  )
}
