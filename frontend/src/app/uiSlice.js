import { createSlice } from '@reduxjs/toolkit'

const initialState = {
  isMobileNavOpen: false,
}

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    openMobileNav(state) {
      state.isMobileNavOpen = true
    },
    closeMobileNav(state) {
      state.isMobileNavOpen = false
    },
    toggleMobileNav(state) {
      state.isMobileNavOpen = !state.isMobileNavOpen
    },
  },
})

export const { openMobileNav, closeMobileNav, toggleMobileNav } = uiSlice.actions

export const selectIsMobileNavOpen = (state) => state.ui.isMobileNavOpen

export default uiSlice.reducer
