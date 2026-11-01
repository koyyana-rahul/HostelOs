import { act, fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { env } from '../../src/lib/env.js'
import { stubFetch } from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

describe('AppLayout and Shell Navigation', () => {
  let originalShowModal
  let originalClose
  let matchMediaListeners = []

  beforeEach(() => {
    stubFetch()
    matchMediaListeners = []

    originalShowModal = HTMLDialogElement.prototype.showModal
    originalClose = HTMLDialogElement.prototype.close

    HTMLDialogElement.prototype.showModal = vi.fn(function () {
      this.open = true
    })
    HTMLDialogElement.prototype.close = vi.fn(function () {
      this.open = false
    })

    window.matchMedia = vi.fn((query) => {
      return {
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn((fn) => matchMediaListeners.push({ query, fn })),
        removeListener: vi.fn((fn) => {
          matchMediaListeners = matchMediaListeners.filter((l) => l.fn !== fn)
        }),
        addEventListener: vi.fn((event, fn) => {
          if (event === 'change') {
            matchMediaListeners.push({ query, fn })
          }
        }),
        removeEventListener: vi.fn((event, fn) => {
          if (event === 'change') {
            matchMediaListeners = matchMediaListeners.filter((l) => l.fn !== fn)
          }
        }),
        dispatchEvent: vi.fn(),
      }
    })
  })

  afterEach(() => {
    HTMLDialogElement.prototype.showModal = originalShowModal
    HTMLDialogElement.prototype.close = originalClose
    document.body.style.overflow = ''
  })

  it('renders skip link targeting #main-content that is focusable', async () => {
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const skipLink = screen.getByRole('link', { name: /skip to content/i })
    expect(skipLink).toBeInTheDocument()
    expect(skipLink).toHaveAttribute('href', '#main-content')

    skipLink.focus()
    expect(document.activeElement).toBe(skipLink)
  })

  it('renders header with workspace title, environment badge, and version tag', async () => {
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    expect(screen.getByText('Operations workspace')).toBeInTheDocument()
    expect(screen.getByText('Hostel management console')).toBeInTheDocument()
    expect(screen.getByText(env.environmentLabel)).toBeInTheDocument()
    expect(screen.getByText(`v${env.appVersion}`)).toBeInTheDocument()
  })

  it('opens drawer, moves focus to close button, and locks body scrolling', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const hamburger = screen.getByRole('button', { name: /open navigation/i })
    expect(hamburger).toHaveAttribute('aria-expanded', 'false')

    await user.click(hamburger)

    const drawer = screen.getByRole('dialog', { name: /application navigation/i })
    expect(drawer).toBeInTheDocument()
    expect(hamburger).toHaveAttribute('aria-expanded', 'true')
    expect(document.body.style.overflow).toBe('hidden')

    const closeBtn = screen.getByRole('button', { name: /close navigation/i })
    expect(document.activeElement).toBe(closeBtn)
  })

  it('dismisses drawer with close button and restores focus to hamburger and restores scroll', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const hamburger = screen.getByRole('button', { name: /open navigation/i })
    await user.click(hamburger)

    const closeBtn = screen.getByRole('button', { name: /close navigation/i })
    await user.click(closeBtn)

    expect(screen.queryByRole('dialog', { name: /application navigation/i })).not.toBeInTheDocument()
    expect(document.body.style.overflow).not.toBe('hidden')
    expect(document.activeElement).toBe(hamburger)
  })

  it('dismisses drawer with Escape key and restores focus to hamburger', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const hamburger = screen.getByRole('button', { name: /open navigation/i })
    await user.click(hamburger)

    expect(screen.getByRole('dialog', { name: /application navigation/i })).toBeInTheDocument()

    fireEvent.keyDown(window, { key: 'Escape' })

    expect(screen.queryByRole('dialog', { name: /application navigation/i })).not.toBeInTheDocument()
    expect(document.body.style.overflow).not.toBe('hidden')
    expect(document.activeElement).toBe(hamburger)
  })

  it('dismisses drawer with backdrop button and restores focus to hamburger', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const hamburger = screen.getByRole('button', { name: /open navigation/i })
    await user.click(hamburger)

    const backdropBtn = screen.getByRole('button', { name: /dismiss navigation/i })
    await user.click(backdropBtn)

    expect(screen.queryByRole('dialog', { name: /application navigation/i })).not.toBeInTheDocument()
    expect(document.activeElement).toBe(hamburger)
  })

  it('closes drawer on navigation without stealing focus back to hamburger', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const hamburger = screen.getByRole('button', { name: /open navigation/i })
    await user.click(hamburger)

    const drawer = screen.getByRole('dialog', { name: /application navigation/i })
    const foundationLink = screen.getAllByRole('link', { name: 'Foundation' }).find(
      (link) => drawer.contains(link),
    )

    await user.click(foundationLink)

    expect(screen.queryByRole('dialog', { name: /application navigation/i })).not.toBeInTheDocument()
    expect(document.body.style.overflow).not.toBe('hidden')
    expect(document.activeElement).not.toBe(hamburger)
  })

  it('closes drawer and restores body scroll when transitioning to desktop breakpoint without focusing hidden hamburger', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/foundation' })

    await screen.findByRole('heading', {
      level: 1,
      name: /a clear base for every module/i,
    })

    const hamburger = screen.getByRole('button', { name: /open navigation/i })
    await user.click(hamburger)

    expect(screen.getByRole('dialog', { name: /application navigation/i })).toBeInTheDocument()
    expect(document.body.style.overflow).toBe('hidden')

    const desktopListener = matchMediaListeners.find((l) => l.query.includes('min-width: 1024px'))
    expect(desktopListener).toBeDefined()

    act(() => {
      desktopListener.fn({ matches: true })
    })

    expect(screen.queryByRole('dialog', { name: /application navigation/i })).not.toBeInTheDocument()
    expect(document.body.style.overflow).not.toBe('hidden')
    expect(document.activeElement).not.toBe(hamburger)
  })
})
