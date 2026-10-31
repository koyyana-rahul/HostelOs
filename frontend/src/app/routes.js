import { RouteError } from '../components/feedback/RouteError.jsx'
import { AppLayout } from '../components/layout/AppLayout.jsx'
import { PublicLayout } from '../components/layout/public/PublicLayout.jsx'
import { AuthGate } from '../features/auth/components/AuthGate.jsx'
import { RequireAuth } from '../features/auth/components/RequireAuth.jsx'
import { RequirePlatform } from '../features/organizations/components/RequirePlatform.jsx'
import { RequirePlatformMembership } from '../features/organizations/components/RequirePlatformMembership.jsx'
import { RequireOrgAccess } from '../features/organizations/components/RequireOrgAccess.jsx'

function lazyPage(load) {
  return {
    lazy: async () => {
      const { default: Component } = await load()
      return { Component }
    },
  }
}

export const routes = [
  {
    path: '/',
    Component: AuthGate,
    ErrorBoundary: RouteError,
    children: [
      {
        Component: PublicLayout,
        children: [
          {
            index: true,
            ...lazyPage(() => import('../features/public/pages/HomePage.jsx')),
          },
          {
            path: 'discover',
            ...lazyPage(() => import('../features/public/pages/DiscoverPage.jsx')),
          },
          {
            path: 'for-owners',
            ...lazyPage(() => import('../features/public/pages/ForOwnersPage.jsx')),
          },
          {
            path: 'features',
            ...lazyPage(() => import('../features/public/pages/FeaturesPage.jsx')),
          },
          {
            path: 'about',
            ...lazyPage(() => import('../features/public/pages/AboutPage.jsx')),
          },
          {
            path: 'contact',
            ...lazyPage(() => import('../features/public/pages/ContactPage.jsx')),
          },
        ],
      },
      {
        path: 'login',
        ...lazyPage(() => import('../features/auth/pages/LoginPage.jsx')),
      },
      {
        path: 'register',
        ...lazyPage(() => import('../features/auth/pages/RegisterPage.jsx')),
      },
      {
        Component: RequirePlatformMembership,
        children: [
          {
            path: 'onboarding',
            ...lazyPage(() => import('../features/organizations/pages/OnboardingPage.jsx')),
          },
          {
            path: 'onboarding/create',
            ...lazyPage(() => import('../features/organizations/pages/CreateOrganizationPage.jsx')),
          },
        ],
      },
      {
        Component: RequirePlatform,
        children: [
          {
            path: 'platform',
            Component: AppLayout,
            children: [
              {
                index: true,
                ...lazyPage(() => import('../features/platform/pages/PlatformDashboardPage.jsx')),
              },
              {
                path: 'organizations',
                ...lazyPage(() => import('../features/platform/pages/PlatformOrganizationsPage.jsx')),
              },
              {
                path: 'users',
                ...lazyPage(() => import('../features/platform/pages/PlatformUsersPage.jsx')),
              },
              {
                path: 'subscriptions',
                ...lazyPage(() => import('../features/platform/pages/PlatformPlaceholderPage.jsx')),
              },
              {
                path: 'settings',
                ...lazyPage(() => import('../features/platform/pages/PlatformPlaceholderPage.jsx')),
              },
            ],
          },
        ],
      },
      {
        Component: RequireAuth,
        children: [
          {
            Component: RequireOrgAccess,
            children: [
              {
                path: 'app',
                Component: AppLayout,
                children: [
                  {
                    index: true,
                    ...lazyPage(() => import('../features/org/pages/OrgOverviewPage.jsx')),
                  },
                  {
                    path: 'overview',
                    ...lazyPage(() => import('../features/org/pages/OrgOverviewPage.jsx')),
                  },
                  {
                    path: 'properties',
                    ...lazyPage(() => import('../features/org/pages/OrgPropertiesPage.jsx')),
                  },
                  {
                    path: 'staff',
                    ...lazyPage(() => import('../features/org/pages/OrgStaffPage.jsx')),
                  },
                  {
                    path: 'operations',
                    ...lazyPage(() => import('../features/org/pages/OrgOperationsPage.jsx')),
                  },
                  {
                    path: 'settings',
                    ...lazyPage(() => import('../features/org/pages/OrgSettingsPage.jsx')),
                  },
                ],
              },
            ],
          },
          {
            Component: AppLayout,
            children: [
              {
                index: true,
                ...lazyPage(() => import('../features/home/pages/HomeRedirectPage.jsx')),
              },
              {
                path: 'foundation',
                ...lazyPage(() => import('../features/foundation/pages/FoundationPage.jsx')),
              },
              {
                path: 'system',
                ...lazyPage(() => import('../features/system/pages/SystemPage.jsx')),
              },
              {
                path: 'unauthorized',
                ...lazyPage(() => import('../components/feedback/AccessDenied.jsx')),
              },
              {
                path: '*',
                ...lazyPage(() => import('./NotFoundPage.jsx')),
              },
            ],
          },
        ],
      },
      {
        path: '*',
        ...lazyPage(() => import('./NotFoundPage.jsx')),
      },
    ],
  },
]
