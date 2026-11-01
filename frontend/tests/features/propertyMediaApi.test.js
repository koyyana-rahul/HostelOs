import { describe, expect, it } from 'vitest'
import { createAppStore } from '../../src/app/store.js'
import { propertyMediaApi } from '../../src/features/organizations/api/propertyMediaApi.js'
import { jsonResponse, stubFetch } from '../helpers/api.js'

describe('property media API', () => {
  it('lists media through the HOSTELOS property media endpoint', async () => {
    const media = [{ id: 7, secureUrl: 'https://cdn.example/image.jpg' }]
    const fetchMock = stubFetch(jsonResponse(media))
    const store = createAppStore()

    const result = await store.dispatch(
      propertyMediaApi.endpoints.listPropertyMedia.initiate({
        organizationId: 3,
        propertyId: 8,
      }),
    )

    const request = fetchMock.mock.calls[0][0]
    expect(new URL(request.url).pathname).toBe(
      '/api/v1/organizations/3/properties/8/media',
    )
    expect(result.data).toEqual(media)
  })
})
