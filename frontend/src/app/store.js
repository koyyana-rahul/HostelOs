import { configureStore } from '@reduxjs/toolkit'
import authReducer from '../features/auth/authSlice.js'
import { baseApi } from '../services/api/baseApi.js'
import uiReducer from './uiSlice.js'
import organizationReducer from '../features/organizations/organizationSlice.js'

export function createAppStore(preloadedState) {
  return configureStore({
    reducer: {
      [baseApi.reducerPath]: baseApi.reducer,
      auth: authReducer,
      ui: uiReducer,
      organization: organizationReducer,
    },
    preloadedState,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(baseApi.middleware),
    devTools: import.meta.env.DEV,
  })
}

export const store = createAppStore()
