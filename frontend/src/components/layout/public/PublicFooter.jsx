import { Link } from 'react-router-dom'
import { Brand } from '../Brand.jsx'

export function PublicFooter() {
  return (
    <footer className="border-t border-line/80 bg-surface">
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-4 lg:px-8">
        <div className="md:col-span-2">
          <Brand />
          <p className="mt-3 max-w-md text-sm text-muted">
            HostelOS helps students and working professionals discover hostels and PGs, and helps owners digitize their operations with a simple, secure platform.
          </p>
        </div>
        <div>
          <p className="text-sm font-semibold text-ink">Product</p>
          <ul className="mt-3 space-y-2 text-sm text-muted">
            <li><Link className="hover:text-ink" to="/discover">Discover</Link></li>
            <li><Link className="hover:text-ink" to="/for-owners">For Owners</Link></li>
            <li><Link className="hover:text-ink" to="/features">Features</Link></li>
          </ul>
        </div>
        <div>
          <p className="text-sm font-semibold text-ink">Company</p>
          <ul className="mt-3 space-y-2 text-sm text-muted">
            <li><Link className="hover:text-ink" to="/about">About</Link></li>
            <li><Link className="hover:text-ink" to="/contact">Contact</Link></li>
            <li><Link className="hover:text-ink" to="/login">Sign in</Link></li>
            <li><Link className="hover:text-ink" to="/register">Register</Link></li>
          </ul>
        </div>
      </div>
      <div className="border-t border-line/80">
        <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-2 px-4 py-4 text-xs text-muted sm:flex-row sm:px-6 lg:px-8">
          <p>© {new Date().getFullYear()} HostelOS. All rights reserved.</p>
          <p>Built for discovery and operations.</p>
        </div>
      </div>
    </footer>
  )
}
