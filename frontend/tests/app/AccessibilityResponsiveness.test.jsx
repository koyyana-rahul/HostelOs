import { act, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { env } from '../../src/lib/env.js'
import { jsonResponse, stubFetch, systemInfoPayload } from '../helpers/api.js'
import { renderApp } from '../test-utils.jsx'

describe('Accessibility, Responsiveness & Regression Verification (F5)', () => {
  let originalShowModal
  let originalClose
  let matchMediaListeners = []

  beforeEach(() => {
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
    vi.restoreAllMocks()
  })

  it('1. skip link is keyboard-accessible and targets #main-content', async () => {
    renderApp({ route: '/' })

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    const skipLink = screen.getByRole('link', { name: /skip to content/i })
    expect(skipLink).toBeInTheDocument()
    expect(skipLink).toHaveAttribute('href', '#main-content')

    skipLink.focus()
    expect(document.activeElement).toBe(skipLink)

    const mainElement = document.getElementById('main-content')
    expect(mainElement).toBeInTheDocument()
    expect(mainElement).toHaveAttribute('tabIndex', '-1')

    mainElement.focus()
    expect(document.activeElement).toBe(mainElement)
  })

  it('2. dialog and mobile drawer handle Escape and restore focus on dismissal', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/' })

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    const hamburger = screen.getByRole('button', { name: /open navigation/i })
    hamburger.focus()
    expect(document.activeElement).toBe(hamburger)

    await user.click(hamburger)

    const drawer = screen.getByRole('dialog', {
      name: /application navigation/i,
    })
    expect(drawer).toBeInTheDocument()

    // Dismiss with Escape key
    await user.keyboard('{Escape}')

    await waitFor(() => {
      expect(
        screen.queryByRole('dialog', { name: /application navigation/i }),
      ).not.toBeInTheDocument()
    })
    expect(document.activeElement).toBe(hamburger)
  })

  describe('3. route semantics across application routes', () => {
    it('verifies heading and landmarks on overview route (/)', async () => {
      renderApp({ route: '/' })
      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /welcome to hostelos/i,
        }),
      ).toBeInTheDocument()
      expect(screen.getByRole('main')).toBeInTheDocument()
      expect(
        screen.getByRole('navigation', { name: /primary/i }),
      ).toBeInTheDocument()
    })

    it('verifies heading and landmarks on foundation route (/foundation)', async () => {
      renderApp({ route: '/foundation' })
      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /a clear base for every module/i,
        }),
      ).toBeInTheDocument()
      expect(screen.getByRole('main')).toBeInTheDocument()
      expect(
        screen.getByRole('navigation', { name: /primary/i }),
      ).toBeInTheDocument()
    })

    it('verifies heading and landmarks on system route (/system)', async () => {
      stubFetch(jsonResponse(systemInfoPayload))
      renderApp({ route: '/system' })
      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /system information/i,
        }),
      ).toBeInTheDocument()
      expect(screen.getByRole('main')).toBeInTheDocument()
      expect(
        screen.getByRole('navigation', { name: /primary/i }),
      ).toBeInTheDocument()
    })

    it('verifies heading and landmarks on unauthorized route (/unauthorized)', async () => {
      renderApp({ route: '/unauthorized' })
      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /access denied/i,
        }),
      ).toBeInTheDocument()
      expect(screen.getByRole('main')).toBeInTheDocument()
      expect(
        screen.getByRole('navigation', { name: /primary/i }),
      ).toBeInTheDocument()
    })

    it('verifies heading and landmarks on login route (/login)', async () => {
      renderApp({ route: '/login', authStatus: 'unauthenticated' })
      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: /sign in to hostelos/i,
        }),
      ).toBeInTheDocument()
      expect(screen.getByRole('main')).toBeInTheDocument()
    })
  })

  it('4. ensures status indicators convey meaning through text/labels and not color alone', async () => {
    renderApp({ route: '/' })

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    // Header environment badge has explicit text
    expect(screen.getByText(env.environmentLabel)).toBeInTheDocument()

    // Metric cards have readable text values and helper descriptions
    const metricsSection = screen.getByRole('region', { name: /key metrics/i })
    expect(within(metricsSection).getByText('85.6%')).toBeInTheDocument()
    expect(within(metricsSection).getByText('Healthy')).toBeInTheDocument()

    // Property occupancy table badges have explicit percentages
    const table = screen.getByRole('table', {
      name: /property occupancy overview/i,
    })
    expect(within(table).getByText('88.0%')).toBeInTheDocument()
    expect(within(table).getByText('85.0%')).toBeInTheDocument()
    expect(within(table).getByText('75.0%')).toBeInTheDocument()
  })

  it('5. responsive drawer automatically dismisses and restores scroll upon desktop breakpoint', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/' })

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    const hamburger = screen.getByRole('button', { name: /open navigation/i })
    await user.click(hamburger)

    expect(
      screen.getByRole('dialog', { name: /application navigation/i }),
    ).toBeInTheDocument()
    expect(document.body.style.overflow).toBe('hidden')

    // Find listener registered for (min-width: 1024px)
    const desktopListener = matchMediaListeners.find(
      (l) => l.query === '(min-width: 1024px)',
    )
    expect(desktopListener).toBeDefined()

    // Trigger media change simulating viewport expansion to >= 1024px
    act(() => {
      desktopListener.fn({ matches: true })
    })

    await waitFor(() => {
      expect(
        screen.queryByRole('dialog', { name: /application navigation/i }),
      ).not.toBeInTheDocument()
    })
    expect(document.body.style.overflow).toBe('')
  })

  it('6. table markup provides semantic column headers with scope and an accessible table name', async () => {
    renderApp({ route: '/' })

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    const table = screen.getByRole('table', {
      name: /property occupancy overview/i,
    })
    expect(table).toBeInTheDocument()

    const colHeaders = within(table).getAllByRole('columnheader')
    expect(colHeaders.length).toBe(4)

    colHeaders.forEach((header) => {
      expect(header).toHaveAttribute('scope', 'col')
    })
  })

  it('7. representative decorative icons are hidden from assistive technology', async () => {
    renderApp({ route: '/' })

    await screen.findByRole('heading', {
      level: 1,
      name: /welcome to hostelos/i,
    })

    const svgs = document.querySelectorAll('svg')
    expect(svgs.length).toBeGreaterThan(0)

    svgs.forEach((svg) => {
      expect(svg).toHaveAttribute('aria-hidden', 'true')
    })
  })
})
