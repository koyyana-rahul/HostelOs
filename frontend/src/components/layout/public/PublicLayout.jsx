import { Outlet } from 'react-router-dom'
import { PublicHeader } from './PublicHeader.jsx'
import { PublicFooter } from './PublicFooter.jsx'

export function PublicLayout() {
  return (
    <div className="min-h-dvh bg-canvas text-ink">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-white"
      >
        Skip to content
      </a>
      <PublicHeader />
      <main id="main-content">
        <Outlet />
      </main>
      <PublicFooter />
    </div>
  )
}
