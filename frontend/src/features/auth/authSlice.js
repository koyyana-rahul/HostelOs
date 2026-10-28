import { createSlice } from '@reduxjs/toolkit'

export const initialAuthState = {
  user: null,
  status: 'checking',
  error: null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState: initialAuthState,
  reducers: {
    sessionCheckStarted(state) {
      state.user = null
      state.status = 'checking'
      state.error = null
    },
    sessionEstablished(state, action) {
      state.user = action.payload
      state.status = 'authenticated'
      state.error = null
    },
    sessionCleared(state) {
      state.user = null
      state.status = 'anonymous'
      state.error = null
    },
    sessionCheckFailed(state) {
      state.user = null
      state.status = 'unavailable'
      state.error = 'unavailable'
    },
  },
})

export const {
  sessionCheckStarted,
  sessionEstablished,
  sessionCleared,
  sessionCheckFailed,
} = authSlice.actions

export const selectCurrentUser = (state) => state.auth.user
export const selectAuthStatus = (state) => state.auth.status
export const selectIsAuthenticated = (state) =>
  state.auth.status === 'authenticated'

export default authSlice.reducer
