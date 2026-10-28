import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router-dom'
import App from '../src/app/App.jsx'
import { routes } from '../src/app/routes.js'
import { createAppStore } from '../src/app/store.js'

export function renderApp({ route = '/' } = {}) {
  const router = createMemoryRouter(routes, {
    initialEntries: [route],
  })
  const store = createAppStore()

  return {
    ...render(<App router={router} store={store} />),
    router,
    store,
  }
}
