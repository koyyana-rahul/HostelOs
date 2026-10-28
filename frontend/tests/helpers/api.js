import { vi } from 'vitest'

export const systemInfoPayload = Object.freeze({
  applicationName: 'HostelOS',
  apiVersion: 'v1',
  status: 'AVAILABLE',
})

export function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function stubFetch(...responses) {
  const fetchMock = vi.fn()

  responses.forEach((response) => {
    if (response instanceof Promise) {
      fetchMock.mockReturnValueOnce(response)
    } else {
      fetchMock.mockResolvedValueOnce(response)
    }
  })

  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
