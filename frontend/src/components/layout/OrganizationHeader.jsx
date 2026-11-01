import { Menu, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import {
  openMobileNav,
  selectIsMobileNavOpen,
} from '../../app/uiSlice.js'
import { useAppDispatch } from '../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../hooks/useAppSelector.js'
import { Button } from '../ui/Button.jsx'

export function OrganizationHeader({ organization, menuButtonRef }) {
  const dispatch = useAppDispatch()
  const isMobileNavOpen = useAppSelector(selectIsMobileNavOpen)

  return (
    <header className="sticky top-0 z-30 border-b border-line/70 bg-canvas/85 backdrop-blur-md">
      <div className="flex h-16 items-center gap-3 px-4 sm:px-6 lg:px-8">
        <button
          ref={menuButtonRef}
          type="button"
          aria-label="Open navigation"
          aria-expanded={isMobileNavOpen}
          aria-controls={isMobileNavOpen ? 'mobile-navigation' : undefined}
          onClick={() => dispatch(openMobileNav())}
          className="grid size-9 shrink-0 place-items-center rounded-lg border border-line bg-surface text-ink transition-colors hover:bg-brand-soft lg:hidden"
        >
          <Menu className="size-4" aria-hidden="true" />
        </button>

        <div className="min-w-0">
          <p className="hidden text-[10px] font-semibold uppercase tracking-[0.2em] text-muted sm:block">
            HostelOS Manage
          </p>
          <p className="truncate text-sm font-medium text-ink">
            {organization?.name || 'Owner workspace'}
          </p>
        </div>

        <div className="ml-auto flex items-center gap-2">
          <Button as={Link} to="/app/account" variant="secondary" size="sm">
            <UserRound className="size-4" aria-hidden="true" />
            <span className="hidden sm:inline">Account</span>
          </Button>
        </div>
      </div>
    </header>
  )
}
