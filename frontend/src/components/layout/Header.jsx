import { Menu } from 'lucide-react'
import {
  openMobileNav,
  selectIsMobileNavOpen,
} from '../../app/uiSlice.js'
import { useAppDispatch } from '../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../hooks/useAppSelector.js'
import { env } from '../../lib/env.js'
import { Badge } from '../ui/Badge.jsx'

export function Header({ menuButtonRef }) {
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
  )
}
