import { useGetMembershipsQuery } from '../../auth/api/authApi.js'
import { useAppDispatch } from '../../../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../../../hooks/useAppSelector.js'
import { selectCurrentOrganizationId, setCurrentOrganization } from '../organizationSlice.js'
import { useEffect } from 'react'

export function useMemberships() {
  const { data: memberships = [], isLoading, isError, refetch } = useGetMembershipsQuery()
  const currentOrgId = useAppSelector(selectCurrentOrganizationId)
  const dispatch = useAppDispatch()

  useEffect(() => {
    if (!currentOrgId && memberships.length > 0) {
      const firstActive = memberships.find((m) => m.status === 'ACTIVE')
      if (firstActive) {
        dispatch(setCurrentOrganization(firstActive.organizationId))
      }
    }
  }, [currentOrgId, memberships, dispatch])

  const activeMemberships = memberships.filter((m) => m.status === 'ACTIVE')
  const currentMembership = activeMemberships.find((m) => m.organizationId === currentOrgId) || activeMemberships[0]

  return {
    memberships,
    activeMemberships,
    currentMembership,
    currentOrgId,
    isLoading,
    isError,
    refetch,
  }
}
