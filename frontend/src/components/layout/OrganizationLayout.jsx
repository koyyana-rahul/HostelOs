import { useEffect, useRef } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import {
  closeMobileNav,
  selectIsMobileNavOpen,
} from '../../app/uiSlice.js'
import { useSessionRevalidation } from '../../features/auth/hooks/useSessionRevalidation.js'
import { useOwnerOrganization } from '../../features/organizations/hooks/useOwnerOrganization.js'
import { useAppDispatch } from '../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../hooks/useAppSelector.js'
import { env } from '../../lib/env.js'
import { MobileNavDrawer } from './MobileNavDrawer.jsx'
import { OrganizationHeader } from './OrganizationHeader.jsx'
import { OrganizationSidebar } from './OrganizationSidebar.jsx'

export function OrganizationLayout() {
  const dispatch = useAppDispatch()
  const isMobileNavOpen = useAppSelector(selectIsMobileNavOpen)
  const location = useLocation()
  const menuButtonRef = useRef(null)
  const { organization } = useOwnerOrganization()

  useSessionRevalidation()

  useEffect(() => {
    dispatch(closeMobileNav())
  }, [location.pathname, dispatch])

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) {
      return undefined
    }

    const mql = window.matchMedia('(min-width: 1024px)')
    const handleMediaChange = (event) => {
      if (event.matches) {
        dispatch(closeMobileNav())
      }
    }

    if (mql.addEventListener) {
      mql.addEventListener('change', handleMediaChange)
    } else {
      mql.addListener(handleMediaChange)
    }

    return () => {
      if (mql.removeEventListener) {
        mql.removeEventListener('change', handleMediaChange)
      } else {
        mql.removeListener(handleMediaChange)
      }
    }
  }, [dispatch])

  return (
    <div className="min-h-dvh bg-canvas lg:grid lg:grid-cols-[272px_minmax(0,1fr)]">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-white"
      >
        Skip to content
      </a>

      <aside className="hidden border-r border-line/80 bg-surface lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col">
        <OrganizationSidebar organization={organization} />
      </aside>

      <MobileNavDrawer
        open={isMobileNavOpen}
        onClose={() => dispatch(closeMobileNav())}
        triggerButtonRef={menuButtonRef}
      >
        <OrganizationSidebar
          organization={organization}
          onNavigate={() => dispatch(closeMobileNav())}
        />
      </MobileNavDrawer>

      <div className="flex min-h-dvh min-w-0 flex-col">
        <OrganizationHeader
          organization={organization}
          menuButtonRef={menuButtonRef}
        />

        <main id="main-content" tabIndex={-1} className="flex-1">
          <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
            <Outlet />
          </div>
        </main>

        <footer className="border-t border-line/70">
          <div className="mx-auto flex w-full max-w-6xl flex-col gap-2 px-4 py-5 text-xs text-muted sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
            <p>
              © {new Date().getFullYear()} {env.appName}. Owner workspace.
            </p>
            <p>{organization?.slug ? `/${organization.slug}` : 'HostelOS Manage'}</p>
          </div>
        </footer>
      </div>
    </div>
  )
}
