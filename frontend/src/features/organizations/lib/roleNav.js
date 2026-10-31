import {
  Briefcase,
  Building2,
  LayoutDashboard,
  Search,
  Settings,
  UserRound,
  UsersRound,
  ClipboardList,
} from 'lucide-react'

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
  { to: '/app/listings', label: 'Listings', icon: Search },
  { to: '/app/enquiries', label: 'Enquiries', icon: Briefcase },
  { to: '/app/settings', label: 'Settings', icon: Settings },
  { to: '/app/account', label: 'Account', icon: UserRound },
]

export const ownerNav = [
  { to: '/app/overview', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/properties', label: 'Properties', icon: Building2 },
  { to: '/app/listings', label: 'Listings', icon: Search },
  { to: '/app/enquiries', label: 'Enquiries', icon: Briefcase },
  { to: '/app/settings', label: 'Settings', icon: Settings },
  { to: '/app/account', label: 'Account', icon: UserRound },
]

export const staffNav = [
  { to: '/app/overview', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/properties', label: 'Properties', icon: Building2 },
  { to: '/app/listings', label: 'Listings', icon: Search },
  { to: '/app/enquiries', label: 'Enquiries', icon: Briefcase },
]
