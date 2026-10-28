import { RouteError } from '../components/feedback/RouteError.jsx'
import { AppLayout } from '../components/layout/AppLayout.jsx'
import { AuthGate } from '../features/auth/components/AuthGate.jsx'
import { RequireAuth } from '../features/auth/components/RequireAuth.jsx'

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
        path: 'login',
        ...lazyPage(() => import('../features/auth/pages/LoginPage.jsx')),
      },
      {
        Component: RequireAuth,
        children: [
          {
            Component: AppLayout,
            children: [
              {
                index: true,
                ...lazyPage(
                  () => import('../features/dashboard/pages/DashboardPage.jsx'),
                ),
              },
              {
                path: 'foundation',
                ...lazyPage(
                  () =>
                    import('../features/foundation/pages/FoundationPage.jsx'),
                ),
              },
              {
                path: 'system',
                ...lazyPage(
                  () => import('../features/system/pages/SystemPage.jsx'),
                ),
              },
              {
                path: '*',
                ...lazyPage(() => import('./NotFoundPage.jsx')),
              },
            ],
          },
        ],
      },
    ],
  },
]
