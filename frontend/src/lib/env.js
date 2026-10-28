const read = (value) => (typeof value === 'string' ? value.trim() : '')

const appName = read(import.meta.env.VITE_APP_NAME) || 'HostelOS'
const configuredApiBaseUrl = read(import.meta.env.VITE_API_BASE_URL) || '/api'
const apiBaseUrl =
  configuredApiBaseUrl === '/' ? '' : configuredApiBaseUrl.replace(/\/+$/, '')
const mode = import.meta.env.MODE
const isDev = Boolean(import.meta.env.DEV)

export const env = Object.freeze({
  appName,
  appVersion: read(import.meta.env.VITE_APP_VERSION) || '0.1.1',
  apiBaseUrl,
  mode,
  isDev,
  environmentLabel: isDev
    ? 'Local development'
    : mode.charAt(0).toUpperCase() + mode.slice(1),
})
