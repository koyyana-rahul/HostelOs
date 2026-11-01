import { RouteError } from '../components/feedback/RouteError.jsx'
import { AppLayout } from '../components/layout/AppLayout.jsx'
import { OrganizationLayout } from '../components/layout/OrganizationLayout.jsx'
import { PublicLayout } from '../components/layout/public/PublicLayout.jsx'
import { AuthGate } from '../features/auth/components/AuthGate.jsx'
import { RequireAuth } from '../features/auth/components/RequireAuth.jsx'
import { RequireCustomer } from '../features/auth/components/RequireCustomer.jsx'
import { RequireOrganizationOwner } from '../features/organizations/components/RequireOrganizationOwner.jsx'
import { RequireOwnerOnboarding } from '../features/organizations/components/RequireOwnerOnboarding.jsx'
import { RequirePlatform } from '../features/organizations/components/RequirePlatform.jsx'

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
            path: 'discover/:listingId',
            ...lazyPage(
              () => import('../features/public/pages/ListingDetailsPage.jsx'),
            ),
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
        Component: RequireOwnerOnboarding,
        children: [
          {
            path: 'onboarding',
            ...lazyPage(() => import('../features/organizations/pages/OnboardingPage.jsx')),
          },
          {
            path: 'onboarding/create',
            ...lazyPage(
              () => import('../features/organizations/pages/CreateOrganizationPage.jsx'),
            ),
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
            Component: PublicLayout,
            children: [
              {
                Component: RequireCustomer,
                children: [
                  {
                    path: 'account',
                    ...lazyPage(
                      () => import('../features/customer/account/AccountPage.jsx'),
                    ),
                    children: [
                      {
                        index: true,
                        ...lazyPage(
                          () => import('../features/customer/account/ProfilePage.jsx'),
                        ),
                      },
                      {
                        path: 'profile',
                        ...lazyPage(
                          () => import('../features/customer/account/ProfilePage.jsx'),
                        ),
                      },
                      {
                        path: 'settings',
                        ...lazyPage(
                          () => import('../features/customer/account/AccountSettingsPage.jsx'),
                        ),
                      },
                    ],
                  },
                  {
                    path: 'my-enquiries',
                    ...lazyPage(
                      () => import('../features/customer/account/MyEnquiriesPage.jsx'),
                    ),
                  },
                  {
                    path: 'my-enquiries/:enquiryId',
                    ...lazyPage(
                      () => import('../features/customer/account/EnquiryDetailsPage.jsx'),
                    ),
                  },
                ],
              },
            ],
          },
          {
            Component: RequireOrganizationOwner,
            children: [
              {
                path: 'app',
                Component: OrganizationLayout,
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
                    path: 'listings',
                    ...lazyPage(() => import('../features/org/pages/OrgListingsPage.jsx')),
                  },
                  {
                    path: 'enquiries',
                    ...lazyPage(() => import('../features/org/pages/OrgEnquiriesPage.jsx')),
                  },
                  {
                    path: 'settings',
                    ...lazyPage(() => import('../features/org/pages/OrgSettingsPage.jsx')),
                  },
                  {
                    path: 'account',
                    ...lazyPage(() => import('../features/org/pages/OrgAccountPage.jsx')),
                  },
                  {
                    path: 'account/profile',
                    ...lazyPage(() => import('../features/org/pages/OrgProfilePage.jsx')),
                  },
                  {
                    path: 'account/settings',
                    ...lazyPage(
                      () => import('../features/org/pages/OrgAccountSettingsPage.jsx'),
                    ),
                  },
                ],
              },
            ],
          },
          {
            Component: AppLayout,
            children: [
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
