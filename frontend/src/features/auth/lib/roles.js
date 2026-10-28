export function normalizeRoleNames(roles) {
  if (!Array.isArray(roles)) {
    return []
  }

  const seen = new Set()

  return roles.reduce((names, role) => {
    if (typeof role !== 'string') {
      return names
    }

    const name = role.trim()

    if (!name || seen.has(name)) {
      return names
    }

    seen.add(name)
    names.push(name)
    return names
  }, [])
}
