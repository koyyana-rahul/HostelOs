export function hasPlatformRole(user) {
  if (!user?.roles) return false
  return Array.isArray(user.roles) && user.roles.includes('PLATFORM_ADMIN')
}

export function getLandingPath(user, memberships = []) {
  if (!user) return '/login'
  if (hasPlatformRole(user)) return '/platform'
  const list = Array.isArray(memberships) ? memberships : []
  const activeMemberships = list.filter((m) => m && m.status === 'ACTIVE')
  if (activeMemberships.length > 0) return '/app'
  return '/onboarding'
}

export function getUserPrimaryRole(user, memberships = []) {
  if (!user) return null
  if (hasPlatformRole(user)) return 'PLATFORM_ADMIN'
  const list = Array.isArray(memberships) ? memberships : []
  const active = list.find((m) => m && m.status === 'ACTIVE')
  if (!active) return 'CUSTOMER'
  if (active.orgRole === 'ORG_ADMIN') return 'ORGANIZATION_ADMIN'
  if (active.orgRole === 'ORG_STAFF') return 'STAFF'
  return active.orgRole
}
