import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { Menu, X } from 'lucide-react'
import { Brand } from '../Brand.jsx'
import { Button } from '../../ui/Button.jsx'

const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/discover', label: 'Discover' },
  { to: '/for-owners', label: 'For Owners' },
  { to: '/features', label: 'Features' },
  { to: '/about', label: 'About' },
  { to: '/contact', label: 'Contact' },
]

export function PublicHeader() {
  const [open, setOpen] = useState(false)

  return (
    <header className="sticky top-0 z-40 border-b border-line/80 bg-surface/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center gap-2">
          <Brand />
        </Link>
        <nav className="hidden items-center gap-6 md:flex">
          {navLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `text-sm font-medium transition-colors ${
                  isActive ? 'text-brand' : 'text-muted hover:text-ink'
                }`
              }
              end={link.to === '/'}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="hidden items-center gap-2 md:flex">
          <Button asChild variant="ghost" size="sm">
            <Link to="/login">Sign in</Link>
          </Button>
          <Button asChild size="sm">
            <Link to="/register">Get started</Link>
          </Button>
        </div>
        <button
          className="inline-flex items-center justify-center rounded-lg p-2 text-muted hover:text-ink md:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label="Toggle navigation"
        >
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </div>
      {open ? (
        <div className="border-t border-line/80 md:hidden">
          <nav className="mx-auto flex max-w-7xl flex-col px-4 py-2 sm:px-6 lg:px-8">
            {navLinks.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  `py-2 text-sm font-medium ${
                    isActive ? 'text-brand' : 'text-muted'
                  }`
                }
                end={link.to === '/'}
              >
                {link.label}
              </NavLink>
            ))}
            <div className="mt-2 flex items-center gap-2 pb-2">
              <Button asChild variant="ghost" size="sm" className="flex-1">
                <Link to="/login">Sign in</Link>
              </Button>
              <Button asChild size="sm" className="flex-1">
                <Link to="/register">Get started</Link>
              </Button>
            </div>
          </nav>
        </div>
      ) : null}
    </header>
  )
}
