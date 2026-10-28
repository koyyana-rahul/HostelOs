import { LayoutDashboard, Layers, Server } from 'lucide-react'

export const navigation = [
  {
    to: '/',
    label: 'Overview',
    icon: LayoutDashboard,
    end: true,
  },
  {
    to: '/foundation',
    label: 'Foundation',
    icon: Layers,
    end: false,
  },
  {
    to: '/system',
    label: 'System',
    icon: Server,
    end: false,
  },
]
