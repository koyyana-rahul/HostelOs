import { describe, expect, it } from 'vitest'
import { createAppStore } from '../../src/app/store.js'
import { systemApi } from '../../src/features/system/api/systemApi.js'
import {
  getSystemInfoErrorMessage,
  validateSystemInfo,
} from '../../src/features/system/lib/systemInfo.js'
import { jsonResponse, stubFetch, systemInfoPayload } from '../helpers/api.js'

describe('system API', () => {
  it('requests /api/v1/system/info through the shared base API', async () => {
    const fetchMock = stubFetch(jsonResponse(systemInfoPayload))
    const store = createAppStore()

    const result = await store.dispatch(
      systemApi.endpoints.getSystemInfo.initiate(),
    )

    expect(fetchMock).toHaveBeenCalledTimes(1)

    const request = fetchMock.mock.calls[0][0]
    const requestUrl = new URL(request.url)

    expect(requestUrl.pathname).toBe('/api/v1/system/info')
    expect(request.method).toBe('GET')
    expect(result.data).toEqual(systemInfoPayload)
  })

  it('returns the backend HTTP error to RTK Query', async () => {
    stubFetch(
      jsonResponse({ message: 'internal database detail' }, 503),
    )
    const store = createAppStore()

    const result = await store.dispatch(
      systemApi.endpoints.getSystemInfo.initiate(),
    )

    expect(result.error.status).toBe(503)
    expect(
      getSystemInfoErrorMessage(result.error, null),
    ).not.toContain('internal database detail')
  })

  it('rejects incomplete metadata without inventing fields', () => {
    const validation = validateSystemInfo({
      applicationName: 'HostelOS',
      apiVersion: 'v1',
    })

    expect(validation.valid).toBe(false)
    expect(validation.message).toMatch(/incomplete/i)
  })

  it('accepts the authoritative system metadata shape unchanged', () => {
    const payload = {
      applicationName: 'HostelOS',
      apiVersion: 'v1',
      status: 'AVAILABLE',
    }

    const validation = validateSystemInfo(payload)

    expect(validation.valid).toBe(true)
    expect(validation.data).toBe(payload)
  })
})
