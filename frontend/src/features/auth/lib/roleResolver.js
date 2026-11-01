/**
 * Frontend navigation role resolution.
 *
 * These classifications are navigation states, not backend security roles.
 * Backend authorization remains authoritative.
 */

export const USER_CLASSIFICATION = Object.freeze({
  ANONYMOUS: 'ANONYMOUS',
  CUSTOMER: 'CUSTOMER',
  ORGANIZATION_OWNER: 'ORGANIZATION_OWNER',
  ORG_STAFF: 'ORG_STAFF',
  PLATFORM_ADMIN: 'PLATFORM_ADMIN',
})

function hasRole(user, role) {
  return Array.isArray(user?.roles) && user.roles.includes(role)
}

export function hasPlatformRole(user) {
  return hasRole(user, 'PLATFORM_ADMIN')
}

export function hasOrganizationRole(user) {
  return hasRole(user, 'ORGANIZATION_ADMIN')
}

export function hasStaffRole(user) {
  return hasRole(user, 'STAFF')
}

export function hasCustomerRole(user) {
  return hasRole(user, 'CUSTOMER')
}

export function hasOwnerOrganization(organizations) {
  return Array.isArray(organizations) && organizations.length > 0
}

export function getActiveMemberships(memberships) {
  const list = Array.isArray(memberships) ? memberships : []
  return list.filter((membership) => membership && membership.status === 'ACTIVE')
}

export function hasOrgRole(memberships, orgRole) {
  return getActiveMemberships(memberships).some(
    (membership) => membership.orgRole === orgRole,
  )
}

export function classifyUser(user, organizations = [], memberships = []) {
  if (!user) {
    return USER_CLASSIFICATION.ANONYMOUS
  }

  if (hasPlatformRole(user)) {
    return USER_CLASSIFICATION.PLATFORM_ADMIN
  }

  if (hasOrganizationRole(user) && hasOwnerOrganization(organizations)) {
    return USER_CLASSIFICATION.ORGANIZATION_OWNER
  }

  if (hasOrganizationRole(user)) {
    return USER_CLASSIFICATION.ORGANIZATION_OWNER
  }

  if (
    hasStaffRole(user) ||
    hasOrgRole(memberships, 'ORG_STAFF') ||
    hasOrgRole(memberships, 'ORGANIZATION_ADMIN')
  ) {
    return USER_CLASSIFICATION.ORG_STAFF
  }

  return USER_CLASSIFICATION.CUSTOMER
}

export function getLandingPath(user, organizations = []) {
  if (!user) {
    return '/login'
  }

  if (hasPlatformRole(user)) {
    return '/platform'
  }

  if (hasOrganizationRole(user)) {
    return hasOwnerOrganization(organizations)
      ? '/app/overview'
      : '/onboarding'
  }

  return '/discover'
}

export function getUserPrimaryRole(user) {
  if (!user) {
    return null
  }

  if (hasPlatformRole(user)) {
    return 'PLATFORM_ADMIN'
  }

  if (hasOrganizationRole(user)) {
    return 'ORGANIZATION_ADMIN'
  }

  if (hasStaffRole(user)) {
    return 'STAFF'
  }

  return 'CUSTOMER'
}

export function isOwner(user, organizations) {
  return hasOrganizationRole(user) && hasOwnerOrganization(organizations)
}

export function isOwnerAccount(user) {
  return hasOrganizationRole(user)
}

export function isCustomerOnly(user) {
  return (
    Boolean(user) &&
    !hasPlatformRole(user) &&
    !hasOrganizationRole(user) &&
    !hasStaffRole(user)
  )
}

export function isStaff(user) {
  return hasStaffRole(user)
}

export function isPlatformAdmin(user) {
  return hasPlatformRole(user)
}
