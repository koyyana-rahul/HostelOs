import { useEffect } from 'react'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { useListMyOrganizationsQuery } from '../api/organizationApi.js'
import {
  selectCurrentOrganizationId,
  setCurrentOrganization,
} from '../organizationSlice.js'

export function useOwnerOrganization() {
  const dispatch = useAppDispatch()
  const currentOrganizationId = useAppSelector(selectCurrentOrganizationId)
  const query = useListMyOrganizationsQuery()
  const organizations = Array.isArray(query.data) ? query.data : []
  const organization =
    organizations.find((item) => item.id === currentOrganizationId) ??
    organizations[0] ??
    null

  useEffect(() => {
    if (organization && organization.id !== currentOrganizationId) {
      dispatch(setCurrentOrganization(organization.id))
    }
  }, [organization, currentOrganizationId, dispatch])

  return {
    ...query,
    organizations,
    organization,
    organizationId: organization?.id ?? null,
  }
}
