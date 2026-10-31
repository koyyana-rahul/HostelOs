import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test-utils.jsx'

describe('Public website pages', () => {
  it('renders Home page for anonymous users', async () => {
    renderApp({ route: '/', authStatus: 'anonymous', user: null })
    expect(await screen.findByRole('heading', { name: /discover hostels and pgs/i })).toBeInTheDocument()
  })

  it('renders Discover page for anonymous users', async () => {
    renderApp({ route: '/discover', authStatus: 'anonymous', user: null })
    expect(await screen.findByRole('heading', { name: /find hostels and pgs/i })).toBeInTheDocument()
  })

  it('renders For Owners page for anonymous users', async () => {
    renderApp({ route: '/for-owners', authStatus: 'anonymous', user: null })
    expect(await screen.findByRole('heading', { name: /digitize and manage your property/i })).toBeInTheDocument()
  })

  it('renders Features page for anonymous users', async () => {
    renderApp({ route: '/features', authStatus: 'anonymous', user: null })
    expect(await screen.findByRole('heading', { name: /built for discovery and operations/i })).toBeInTheDocument()
  })

  it('renders About page for anonymous users', async () => {
    renderApp({ route: '/about', authStatus: 'anonymous', user: null })
    expect(await screen.findByRole('heading', { name: /about hostelos/i })).toBeInTheDocument()
  })

  it('renders Contact page for anonymous users', async () => {
    renderApp({ route: '/contact', authStatus: 'anonymous', user: null })
    expect(await screen.findByRole('heading', { name: /get in touch/i })).toBeInTheDocument()
  })

  it('has navigation links to public pages', async () => {
    renderApp({ route: '/', authStatus: 'anonymous', user: null })
    expect(
      (await screen.findAllByRole('link', { name: /discover/i })).length,
    ).toBeGreaterThan(0)
    expect(
      screen.getAllByRole('link', { name: /for owners/i }).length,
    ).toBeGreaterThan(0)
    expect(
      screen.getAllByRole('link', { name: /^features$/i }).length,
    ).toBeGreaterThan(0)
  })

  it('has CTA to find hostel and list property', async () => {
    renderApp({ route: '/', authStatus: 'anonymous', user: null })
    expect(await screen.findByRole('link', { name: /find a hostel or pg/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /list your property/i })).toBeInTheDocument()
  })
})
