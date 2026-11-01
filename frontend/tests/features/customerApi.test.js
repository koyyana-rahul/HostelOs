import { describe, expect, it } from 'vitest'
import { createAppStore } from '../../src/app/store.js'
import { customerApi } from '../../src/features/customer/api/customerApi.js'
import { csrfPayload, jsonResponse, stubFetch } from '../helpers/api.js'
import { createTestAuthState } from '../test-utils.jsx'

describe('customer API session handling', () => {
  it('clears the session when a CSRF-protected profile mutation returns 401', async () => {
    stubFetch(jsonResponse(csrfPayload), jsonResponse({ status: 401 }, 401))
    const store = createAppStore({
      auth: createTestAuthState('authenticated', {
        id: 8,
        loginIdentifier: 'customer@example.com',
        displayName: 'Customer One',
        roles: ['CUSTOMER'],
      }),
    })

    const result = await store.dispatch(
      customerApi.endpoints.updateMyProfile.initiate({ displayName: 'Updated' }),
    )

    expect(result.error.status).toBe(401)
    expect(store.getState().auth.status).toBe('anonymous')
    expect(store.getState().auth.user).toBeNull()
  })
})
