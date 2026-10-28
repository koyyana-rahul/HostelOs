import { RouteError } from '../components/feedback/RouteError.jsx'
import { AppLayout } from '../components/layout/AppLayout.jsx'

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
    Component: AppLayout,
    ErrorBoundary: RouteError,
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
          () => import('../features/foundation/pages/FoundationPage.jsx'),
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
]
