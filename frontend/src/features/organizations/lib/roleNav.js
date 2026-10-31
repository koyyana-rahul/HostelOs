import { LayoutDashboard, Building2, UsersRound, ClipboardList, Settings } from 'lucide-react'

export const platformNav = [
  { to: '/platform', label: 'Platform Dashboard', icon: LayoutDashboard, end: true },
  { to: '/platform/organizations', label: 'Organizations', icon: Building2 },
  { to: '/platform/users', label: 'Users', icon: UsersRound },
  { to: '/platform/subscriptions', label: 'Subscription Management', icon: ClipboardList },
  { to: '/platform/settings', label: 'Platform Settings', icon: Settings },
]

export const orgNav = [
  { to: '/app/overview', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/properties', label: 'Properties', icon: Building2 },
  { to: '/app/staff', label: 'Staff and Members', icon: UsersRound },
  { to: '/app/operations', label: 'Operations', icon: ClipboardList },
  { to: '/app/settings', label: 'Settings', icon: Settings },
]

export const staffNav = [
  { to: '/app/overview', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/properties', label: 'Properties', icon: Building2 },
  { to: '/app/operations', label: 'Operations', icon: ClipboardList },
]
