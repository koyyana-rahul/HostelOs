import { useEffect, useRef, useState } from 'react'
import {
  AlertCircle,
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  ImagePlus,
  LoaderCircle,
  Star,
  Trash2,
  UploadCloud,
} from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import {
  useCompleteMediaUploadMutation,
  useCreateMediaUploadIntentMutation,
  useDeletePropertyMediaMutation,
  useListPropertyMediaQuery,
  useReorderPropertyMediaMutation,
  useSelectPrimaryMediaMutation,
} from '../../organizations/api/propertyMediaApi.js'

const MAX_FILE_BYTES = 10 * 1024 * 1024
const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

function messageFromError(error, fallback) {
  return error?.data?.message || fallback
}

function uploadToProvider(instructions, file, onProgress) {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest()
    request.open(instructions.method, instructions.uploadUrl)
    request.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100))
    }
    request.onerror = () => reject(new Error('The image upload failed.'))
    request.onload = () => {
      let response
      try {
        response = JSON.parse(request.responseText)
      } catch {
        reject(new Error('The image provider returned an invalid response.'))
        return
      }
      if (request.status < 200 || request.status >= 300) {
        reject(new Error(response?.error?.message || 'The image upload failed.'))
        return
      }
      resolve(response)
    }
    const formData = new FormData()
    Object.entries(instructions.parameters).forEach(([key, value]) => formData.append(key, value))
    formData.append('file', file)
    request.send(formData)
  })
}

export function PropertyMediaManager({ organizationId, propertyId }) {
  const inputRef = useRef(null)
  const previewsRef = useRef([])
  const [selectedFiles, setSelectedFiles] = useState([])
  const [dragging, setDragging] = useState(false)
  const [actionError, setActionError] = useState(null)
  const [uploadIntent] = useCreateMediaUploadIntentMutation()
  const [completeUpload] = useCompleteMediaUploadMutation()
  const [selectPrimary, { isLoading: isSelectingPrimary }] = useSelectPrimaryMediaMutation()
  const [reorder, { isLoading: isReordering }] = useReorderPropertyMediaMutation()
  const [deleteMedia, { isLoading: isDeleting }] = useDeletePropertyMediaMutation()
  const {
    data: media = [],
    isLoading: isLoadingMedia,
    isError: isMediaError,
    refetch,
  } = useListPropertyMediaQuery({ organizationId, propertyId })

  useEffect(() => () => {
    previewsRef.current.forEach((preview) => URL.revokeObjectURL(preview))
  }, [])

  const addFiles = (files) => {
    setActionError(null)
    const next = Array.from(files).map((file) => {
      const error = !ALLOWED_TYPES.has(file.type)
        ? 'Use JPEG, PNG, or WebP images.'
        : file.size > MAX_FILE_BYTES
          ? 'Images must be 10 MiB or smaller.'
          : null
      const preview = URL.createObjectURL(file)
      previewsRef.current.push(preview)
      return {
        id: `${file.name}-${file.lastModified}-${Math.random()}`,
        file,
        preview,
        status: error ? 'failed' : 'queued',
        progress: 0,
        error,
      }
    })
    setSelectedFiles((current) => [...current, ...next])
  }

  const updateFile = (id, patch) => {
    setSelectedFiles((current) => current.map((item) => item.id === id ? { ...item, ...patch } : item))
  }

  const uploadFile = async (item) => {
    updateFile(item.id, { status: 'uploading', progress: 0, error: null })
    try {
      const intent = await uploadIntent({
        organizationId,
        propertyId,
        mimeType: item.file.type,
        byteSize: item.file.size,
      }).unwrap()
      const providerAsset = await uploadToProvider(
        intent,
        item.file,
        (progress) => updateFile(item.id, { progress }),
      )
      const completed = await completeUpload({
        organizationId,
        propertyId,
        mediaId: intent.mediaId,
        providerAssetId: providerAsset.asset_id,
        providerAssetUrl: providerAsset.secure_url,
      }).unwrap()
      updateFile(item.id, { status: 'complete', progress: 100, mediaId: completed.id })
    } catch (error) {
      updateFile(item.id, {
        status: 'failed',
        error: messageFromError(error, error.message || 'Could not upload this image.'),
      })
    }
  }

  const uploadQueued = () => {
    selectedFiles.filter((item) => item.status === 'queued').forEach(uploadFile)
  }

  const move = async (index, direction) => {
    const next = [...media]
    const target = index + direction
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    try {
      await reorder({
        organizationId,
        propertyId,
        mediaIds: next.map((item) => item.id),
      }).unwrap()
    } catch (error) {
      setActionError(messageFromError(error, 'Could not reorder images.'))
    }
  }

  const remove = async (item) => {
    if (!window.confirm('Delete this property image?')) return
    try {
      await deleteMedia({ organizationId, propertyId, mediaId: item.id }).unwrap()
    } catch (error) {
      setActionError(messageFromError(error, 'Could not delete the image.'))
    }
  }

  return (
    <Card className="mt-4 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="font-semibold text-ink">Property images</h3>
          <p className="text-xs text-muted">JPEG, PNG, or WebP up to 10 MiB each.</p>
        </div>
        <Button size="sm" onClick={() => inputRef.current?.click()}>
          <ImagePlus className="size-4" aria-hidden="true" />
          Choose images
        </Button>
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          className="hidden"
          onChange={(event) => addFiles(event.target.files)}
        />
      </div>

      <div
        className={`mt-4 rounded-xl border-2 border-dashed p-5 text-center text-sm text-muted ${dragging ? 'border-accent bg-accent/5' : 'border-line'}`}
        onDragOver={(event) => { event.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => { event.preventDefault(); setDragging(false); addFiles(event.dataTransfer.files) }}
      >
        <UploadCloud className="mx-auto size-6" aria-hidden="true" />
        Drop images here or use the file picker.
      </div>

      {selectedFiles.length > 0 ? (
        <div className="mt-4 space-y-2">
          {selectedFiles.map((item) => (
            <div key={item.id} className="flex items-center gap-3 rounded-lg border border-line p-2">
              <img src={item.preview} alt="" className="size-12 rounded object-cover" />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm text-ink">{item.file.name}</p>
                {item.status === 'uploading' ? <progress className="mt-1 h-1 w-full" value={item.progress} max="100" /> : null}
                {item.error ? <p className="text-xs text-danger">{item.error}</p> : null}
              </div>
              {item.status === 'complete' ? <CheckCircle2 className="size-4 text-success" aria-label="Uploaded" /> : null}
              {item.status === 'failed' ? <Button size="sm" variant="secondary" onClick={() => uploadFile(item)}>Retry</Button> : null}
            </div>
          ))}
          {selectedFiles.some((item) => item.status === 'queued') ? (
            <Button size="sm" onClick={uploadQueued}>Upload selected images</Button>
          ) : null}
        </div>
      ) : null}

      {actionError ? <p role="alert" className="mt-3 text-sm text-danger">{actionError}</p> : null}
      {isMediaError ? (
        <div className="mt-4 flex items-center justify-between text-sm text-danger">
          <span><AlertCircle className="mr-1 inline size-4" aria-hidden="true" />Could not load images.</span>
          <Button size="sm" variant="secondary" onClick={refetch}>Retry</Button>
        </div>
      ) : isLoadingMedia ? (
        <LoaderCircle className="mx-auto mt-6 size-5 animate-spin text-muted" aria-label="Loading images" />
      ) : media.length === 0 ? (
        <p className="mt-5 text-center text-sm text-muted">No images uploaded yet.</p>
      ) : (
        <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {media.map((item, index) => (
            <div key={item.id} className="group relative overflow-hidden rounded-xl border border-line">
              <img src={item.secureUrl} alt={`${item.format} property image`} className="aspect-square w-full object-cover" />
              {item.primary ? <span className="absolute left-2 top-2 rounded-full bg-ink px-2 py-1 text-xs text-white"><Star className="mr-1 inline size-3" />Primary</span> : null}
              <div className="flex items-center justify-between gap-1 p-2">
                {!item.primary ? <Button size="sm" variant="ghost" disabled={isSelectingPrimary} onClick={() => selectPrimary({ organizationId, propertyId, mediaId: item.id })}>Make primary</Button> : <span />}
                <span className="flex gap-1">
                  <Button size="sm" variant="ghost" disabled={isReordering || index === 0} onClick={() => move(index, -1)} aria-label="Move image up"><ArrowUp className="size-3.5" /></Button>
                  <Button size="sm" variant="ghost" disabled={isReordering || index === media.length - 1} onClick={() => move(index, 1)} aria-label="Move image down"><ArrowDown className="size-3.5" /></Button>
                  <Button size="sm" variant="ghost" disabled={isDeleting} onClick={() => remove(item)} aria-label="Delete image"><Trash2 className="size-3.5" /></Button>
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}
