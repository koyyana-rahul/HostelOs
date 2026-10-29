import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router-dom'
import App from '../src/app/App.jsx'
import { routes } from '../src/app/routes.js'
import { createAppStore } from '../src/app/store.js'
import {
  authUserPayload,
  organizationAdminUserPayload,
  staffUserPayload,
} from './helpers/api.js'

export const authenticatedUser = authUserPayload
export const organizationAdminUser = organizationAdminUserPayload
export const staffUser = staffUserPayload

export function createTestAuthState(status = 'authenticated', user) {
  return {
    user: status === 'authenticated' ? user ?? authenticatedUser : null,
    status,
    error: status === 'unavailable' ? 'unavailable' : null,
  }
}

export function renderApp({
  route = '/',
  authStatus = 'authenticated',
  user,
  authState,
} = {}) {
  const router = createMemoryRouter(routes, {
    initialEntries: [route],
  })
  const store = createAppStore({
    auth: authState ?? createTestAuthState(authStatus, user),
  })

  return {
    ...render(<App router={router} store={store} />),
    router,
    store,
  }
}
