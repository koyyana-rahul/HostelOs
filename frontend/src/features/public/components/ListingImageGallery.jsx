import { ImageOff } from 'lucide-react'
import { useState } from 'react'

function ImageFallback({ label = 'No image available', className = '' }) {
  return (
    <div className={`grid place-items-center bg-gradient-to-br from-brand-soft via-canvas to-surface text-muted ${className}`}>
      <span className="flex items-center gap-2 text-xs">
        <ImageOff className="size-4" aria-hidden="true" />
        {label}
      </span>
    </div>
  )
}

export function ListingCardImage({ listing }) {
  const image = listing.primaryImage || listing.images?.[0]
  const [failed, setFailed] = useState(false)

  if (!image || failed) {
    return <ImageFallback className="h-40" />
  }

  return (
    <img
      src={image.url}
      alt={image.altText || listing.title}
      loading="lazy"
      onError={() => setFailed(true)}
      className="h-40 w-full object-cover"
    />
  )
}

export function ListingImageGallery({ listing }) {
  const images = listing.images || []
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [failed, setFailed] = useState(false)
  const selected = images[selectedIndex]

  if (!selected || failed) {
    return <ImageFallback className="aspect-[16/9] w-full rounded-xl" />
  }

  const selectPrevious = () => {
    setSelectedIndex((index) => (index - 1 + images.length) % images.length)
    setFailed(false)
  }

  const selectNext = () => {
    setSelectedIndex((index) => (index + 1) % images.length)
    setFailed(false)
  }

  return (
    <div className="space-y-3">
      <div className="relative overflow-hidden rounded-xl bg-canvas">
        <img
          src={selected.url}
          alt={selected.altText || listing.title}
          className="aspect-[16/9] w-full object-cover"
          onError={() => setFailed(true)}
        />
        {images.length > 1 ? (
          <>
            <button type="button" aria-label="Previous image" onClick={selectPrevious} className="absolute left-3 top-1/2 rounded-full bg-ink/70 px-3 py-2 text-white">‹</button>
            <button type="button" aria-label="Next image" onClick={selectNext} className="absolute right-3 top-1/2 rounded-full bg-ink/70 px-3 py-2 text-white">›</button>
          </>
        ) : null}
      </div>
      {images.length > 1 ? (
        <div className="flex gap-2 overflow-x-auto pb-1" aria-label="Image thumbnails">
          {images.map((image, index) => (
            <button
              type="button"
              key={`${image.url}-${image.displayOrder}`}
              aria-label={`View image ${index + 1}`}
              aria-pressed={index === selectedIndex}
              onClick={() => { setSelectedIndex(index); setFailed(false) }}
              className={`shrink-0 overflow-hidden rounded-lg border-2 ${index === selectedIndex ? 'border-brand' : 'border-transparent'}`}
            >
              <img src={image.url} alt="" loading="lazy" className="size-16 object-cover" />
            </button>
          ))}
        </div>
      ) : null}
    </div>
  )
}
