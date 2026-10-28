import { configureStore } from '@reduxjs/toolkit'
import { baseApi } from '../services/api/baseApi.js'
import uiReducer from './uiSlice.js'

export function createAppStore() {
  return configureStore({
    reducer: {
      [baseApi.reducerPath]: baseApi.reducer,
      ui: uiReducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(baseApi.middleware),
    devTools: import.meta.env.DEV,
  })
}

export const store = createAppStore()
