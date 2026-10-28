import { PageLoader } from '../components/feedback/PageLoader.jsx'
import { RouterProvider } from 'react-router-dom'
import { AppProviders } from './providers.jsx'
import { router as defaultRouter } from './router.jsx'
import { store as defaultStore } from './store.js'

export default function App({ router = defaultRouter, store = defaultStore }) {
  return (
    <AppProviders store={store}>
      <RouterProvider router={router} fallbackElement={<PageLoader />} />
    </AppProviders>
  )
}
