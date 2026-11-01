import { Provider } from 'react-redux'
import { store as defaultStore } from './store.js'

export function AppProviders({ children, store = defaultStore }) {
  return <Provider store={store}>{children}</Provider>
}