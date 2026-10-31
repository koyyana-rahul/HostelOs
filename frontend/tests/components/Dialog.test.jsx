import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Dialog, DialogFooter, DialogHeader } from '../../src/components/ui/Dialog.jsx'

describe('Dialog component', () => {
  let originalShowModal
  let originalClose

  beforeEach(() => {
    originalShowModal = HTMLDialogElement.prototype.showModal
    originalClose = HTMLDialogElement.prototype.close

    HTMLDialogElement.prototype.showModal = vi.fn(function () {
      this.open = true
    })
    HTMLDialogElement.prototype.close = vi.fn(function () {
      this.open = false
    })
  })

  afterEach(() => {
    HTMLDialogElement.prototype.showModal = originalShowModal
    HTMLDialogElement.prototype.close = originalClose
    document.body.style.overflow = ''
  })

  it('does not render when open is false', () => {
    render(
      <Dialog open={false} title="Hidden Modal" onClose={() => {}}>
        <p>Dialog content</p>
      </Dialog>,
    )
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('renders with accessible name and description when open is true', () => {
    render(
      <Dialog
        open
        title="Hostel Room Details"
        description="Review occupancy and capacity information."
        onClose={() => {}}
      >
        <p>Content inside modal</p>
      </Dialog>,
    )

    const dialog = screen.getByRole('dialog', { name: 'Hostel Room Details' })
    expect(dialog).toBeInTheDocument()
    expect(
      screen.getByText('Review occupancy and capacity information.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Content inside modal')).toBeInTheDocument()
  })

  it('calls showModal on open when supported by the browser', () => {
    render(
      <Dialog open title="Modal Call Test" onClose={() => {}}>
        <p>Testing showModal</p>
      </Dialog>,
    )

    expect(HTMLDialogElement.prototype.showModal).toHaveBeenCalledTimes(1)
  })

  it('calls onClose when close button is clicked', async () => {
    const user = userEvent.setup()
    const handleClose = vi.fn()

    render(
      <Dialog open title="Test Dialog" onClose={handleClose}>
        <p>Close button test</p>
      </Dialog>,
    )

    const closeButton = screen.getByRole('button', { name: 'Close dialog' })
    await user.click(closeButton)
    expect(handleClose).toHaveBeenCalledTimes(1)
  })

  it('calls onClose when Escape key is pressed', () => {
    const handleClose = vi.fn()

    render(
      <Dialog open title="Escape Key Test" onClose={handleClose}>
        <p>Press escape to close</p>
      </Dialog>,
    )

    fireEvent.keyDown(window, { key: 'Escape' })
    expect(handleClose).toHaveBeenCalledTimes(1)
  })

  it('locks body scroll while open and restores it when closed', () => {
    document.body.style.overflow = 'auto'

    const { rerender } = render(
      <Dialog open title="Scroll Lock Test" onClose={() => {}}>
        <p>Modal body</p>
      </Dialog>,
    )

    expect(document.body.style.overflow).toBe('hidden')

    rerender(
      <Dialog open={false} title="Scroll Lock Test" onClose={() => {}}>
        <p>Modal body</p>
      </Dialog>,
    )

    expect(document.body.style.overflow).toBe('auto')
  })

  it('restores focus to the element that had focus before opening', async () => {
    function DialogHarness() {
      const [isOpen, setIsOpen] = useState(false)
      return (
        <div>
          <button
            type="button"
            data-testid="trigger-btn"
            onClick={() => setIsOpen(true)}
          >
            Open Modal
          </button>
          <Dialog
            open={isOpen}
            title="Focus Restoration Test"
            onClose={() => setIsOpen(false)}
          >
            <p>Focus test</p>
          </Dialog>
        </div>
      )
    }

    const user = userEvent.setup()
    render(<DialogHarness />)

    const triggerBtn = screen.getByTestId('trigger-btn')
    triggerBtn.focus()
    expect(document.activeElement).toBe(triggerBtn)

    await user.click(triggerBtn)
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    const closeBtn = screen.getByRole('button', { name: 'Close dialog' })
    await user.click(closeBtn)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(document.activeElement).toBe(triggerBtn)
  })

  it('renders DialogHeader and DialogFooter subcomponents properly', () => {
    render(
      <Dialog open title="Structured Dialog" onClose={() => {}}>
        <DialogHeader
          title="Section Heading"
          description="Section details description"
        />
        <p>Body copy</p>
        <DialogFooter>
          <button type="button">Cancel</button>
          <button type="button">Save</button>
        </DialogFooter>
      </Dialog>,
    )

    expect(screen.getByText('Section Heading')).toBeInTheDocument()
    expect(screen.getByText('Section details description')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument()
  })

  it('calls onClose when clicking outside the dialog content (backdrop)', () => {
    const handleClose = vi.fn()

    render(
      <Dialog open title="Backdrop Click Test" onClose={handleClose}>
        <p>Inside dialog</p>
      </Dialog>,
    )

    const dialog = screen.getByRole('dialog')
    vi.spyOn(dialog, 'getBoundingClientRect').mockReturnValue({
      top: 100,
      left: 100,
      width: 400,
      height: 300,
      bottom: 400,
      right: 500,
    })

    // Click outside the bounding box
    fireEvent.click(dialog, { clientX: 50, clientY: 50 })
    expect(handleClose).toHaveBeenCalledTimes(1)

    // Click inside the bounding box
    fireEvent.click(dialog, { clientX: 200, clientY: 200 })
    expect(handleClose).toHaveBeenCalledTimes(1)
  })

  it('does not close or reopen the modal when parent re-renders with a new inline onClose', async () => {
    function ParentHarness() {
      const [text, setText] = useState('')
      return (
        <div>
          <Dialog
            open
            title="Inline Callback Test"
            onClose={() => {}}
          >
            <input
              data-testid="in-dialog-input"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
          </Dialog>
        </div>
      )
    }

    const user = userEvent.setup()
    render(<ParentHarness />)

    expect(HTMLDialogElement.prototype.showModal).toHaveBeenCalledTimes(1)
    expect(HTMLDialogElement.prototype.close).not.toHaveBeenCalled()

    const input = screen.getByTestId('in-dialog-input')
    await user.type(input, 'hello')

    // showModal should still have been called only once, close 0 times
    expect(HTMLDialogElement.prototype.showModal).toHaveBeenCalledTimes(1)
    expect(HTMLDialogElement.prototype.close).not.toHaveBeenCalled()

    // Focus must remain on the in-dialog input
    expect(document.activeElement).toBe(input)
  })

  it('invokes the latest onClose callback after parent re-renders', async () => {
    const user = userEvent.setup()
    const firstCallback = vi.fn()
    const secondCallback = vi.fn()

    function DynamicHarness() {
      const [useSecond, setUseSecond] = useState(false)
      return (
        <div>
          <button
            type="button"
            data-testid="switch-btn"
            onClick={() => setUseSecond(true)}
          >
            Switch
          </button>
          <Dialog
            open
            title="Latest Callback Test"
            onClose={useSecond ? secondCallback : firstCallback}
          >
            <p>Body</p>
          </Dialog>
        </div>
      )
    }

    render(<DynamicHarness />)

    // Switch callback
    await user.click(screen.getByTestId('switch-btn'))

    // Press Escape
    fireEvent.keyDown(window, { key: 'Escape' })

    expect(firstCallback).not.toHaveBeenCalled()
    expect(secondCallback).toHaveBeenCalledTimes(1)
  })

  it('restores focus and prior body overflow style upon unmounting', () => {
    document.body.style.overflow = 'scroll'

    function MountHarness({ mounted }) {
      return (
        <div>
          <button type="button" data-testid="opener">
            Opener
          </button>
          {mounted ? (
            <Dialog open title="Unmount Test" onClose={() => {}}>
              <p>Modal</p>
            </Dialog>
          ) : null}
        </div>
      )
    }

    const { rerender } = render(<MountHarness mounted={false} />)
    const opener = screen.getByTestId('opener')
    opener.focus()
    expect(document.activeElement).toBe(opener)

    rerender(<MountHarness mounted={true} />)
    expect(document.body.style.overflow).toBe('hidden')

    rerender(<MountHarness mounted={false} />)
    expect(document.body.style.overflow).toBe('scroll')
    expect(document.activeElement).toBe(opener)
  })
})
