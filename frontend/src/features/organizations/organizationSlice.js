import { createSlice } from '@reduxjs/toolkit'

const initialState = {
  currentOrganizationId: null,
}

const organizationSlice = createSlice({
  name: 'organization',
  initialState,
  reducers: {
    setCurrentOrganization: (state, action) => {
      state.currentOrganizationId = action.payload ?? null
    },
    clearCurrentOrganization: (state) => {
      state.currentOrganizationId = null
    },
  },
})

export const { setCurrentOrganization, clearCurrentOrganization } = organizationSlice.actions
export const selectCurrentOrganizationId = (state) => state.organization.currentOrganizationId
export default organizationSlice.reducer
