const requiredFields = ['applicationName', 'apiVersion', 'status']

function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0
}

export function validateSystemInfo(payload) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return {
      valid: false,
      message:
        'The backend returned an unexpected response. Expected a JSON object with system metadata.',
    }
  }

  const missingFields = requiredFields.filter(
    (field) => !isNonEmptyString(payload[field]),
  )

  if (missingFields.length > 0) {
    return {
      valid: false,
      message:
        'The backend response is incomplete. It must include applicationName, apiVersion, and status.',
    }
  }

  return { valid: true, data: payload }
}

export function getSystemInfoErrorMessage(error, validation) {
  if (validation && !validation.valid) {
    return validation.message
  }

  if (!error) {
    return 'The system information request did not complete. Try again.'
  }

  if (error.status === 'FETCH_ERROR') {
    return 'The backend could not be reached. Confirm it is running and that the Vite proxy target is correct.'
  }

  if (error.status === 'TIMEOUT_ERROR') {
    return 'The backend did not respond in time. Try again.'
  }

  if (error.status === 'PARSING_ERROR') {
    return 'The backend returned a response that could not be read.'
  }

  if (error.status === 401 || error.status === 403) {
    return 'The backend rejected the system information request. This endpoint must be public before the page can load.'
  }

  if (error.status === 404) {
    return 'The system information endpoint was not found on the backend.'
  }

  if (typeof error.status === 'number') {
    return 'The backend returned an error while loading system information. Try again.'
  }

  return 'The system information request failed unexpectedly. Try again.'
}

export function getStatusTone(status) {
  const normalized = status.trim().toUpperCase()

  if (['UP', 'OK', 'HEALTHY', 'AVAILABLE'].includes(normalized)) {
    return 'success'
  }

  if (['DEGRADED', 'WARNING'].includes(normalized)) {
    return 'warning'
  }

  if (['DOWN', 'ERROR', 'FAILED'].includes(normalized)) {
    return 'danger'
  }

  return 'neutral'
}
