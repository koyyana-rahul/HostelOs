const properties = [
  {
    id: 'prop-1',
    name: 'Green Valley Hostel',
    location: 'North Campus',
    totalBeds: 100,
    occupiedBeds: 88,
    availableBeds: 12,
    occupancyRate: 88.0,
    tone: 'success',
  },
  {
    id: 'prop-2',
    name: 'Downtown Scholars Residency',
    location: 'City Center',
    totalBeds: 60,
    occupiedBeds: 51,
    availableBeds: 9,
    occupancyRate: 85.0,
    tone: 'success',
  },
  {
    id: 'prop-3',
    name: 'Tech Park Executive Stay',
    location: 'Innovation Hub',
    totalBeds: 20,
    occupiedBeds: 15,
    availableBeds: 5,
    occupancyRate: 75.0,
    tone: 'warning',
  },
]

const totalProperties = properties.length
const totalBeds = properties.reduce((acc, p) => acc + p.totalBeds, 0)
const occupiedBeds = properties.reduce((acc, p) => acc + p.occupiedBeds, 0)
const availableBeds = properties.reduce((acc, p) => acc + p.availableBeds, 0)
const occupancyRate = Number(((occupiedBeds / totalBeds) * 100).toFixed(1))
const activeResidents = 152
const pendingCheckIns = 6
const pendingCheckOuts = 4

const recentActivity = [
  {
    id: 'act-1',
    type: 'CHECK_IN',
    title: 'Resident check-in confirmed',
    detail: 'Room 204 · Green Valley Hostel',
    timestamp: '10 minutes ago',
  },
  {
    id: 'act-2',
    type: 'MAINTENANCE',
    title: 'Work order dispatched',
    detail: 'HVAC filter replacement · Tech Park',
    timestamp: '35 minutes ago',
  },
  {
    id: 'act-3',
    type: 'CHECK_OUT',
    title: 'Resident check-out completed',
    detail: 'Room 108 · Downtown Scholars',
    timestamp: '2 hours ago',
  },
  {
    id: 'act-4',
    type: 'CHECK_IN',
    title: 'Resident check-in confirmed',
    detail: 'Room 312 · Green Valley Hostel',
    timestamp: '4 hours ago',
  },
]

export const mockDashboardData = Object.freeze({
  isMock: true,
  metrics: Object.freeze({
    totalProperties,
    totalBeds,
    occupiedBeds,
    availableBeds,
    occupancyRate,
    activeResidents,
    pendingCheckIns,
    pendingCheckOuts,
  }),
  properties: Object.freeze(properties.map((p) => Object.freeze({ ...p }))),
  recentActivity: Object.freeze(recentActivity.map((a) => Object.freeze({ ...a }))),
})
