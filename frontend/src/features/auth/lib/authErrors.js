const networkMessage =
  'The authentication service could not be reached. Confirm that the backend is running and try again.'

const timeoutMessage =
  'The authentication service did not respond in time. Try again.'

const securityMessage =
  'The security check could not be completed. Refresh the page and try again.'

const serverMessage =
  'The authentication service is unavailable right now. Try again shortly.'

const unexpectedMessage =
  'The authentication request failed unexpectedly. Try again.'

export function getLoginErrorMessage(error) {
  if (!error) {
    return 'The sign-in request did not complete. Try again.'
  }

  if (error.status === 'FETCH_ERROR') {
    return networkMessage
  }

  if (error.status === 'TIMEOUT_ERROR') {
    return timeoutMessage
  }

  if (error.status === 'PARSING_ERROR') {
    return 'The authentication service returned a response that could not be read. Try again.'
  }

  if (error.status === 400) {
    return 'Enter a valid login identifier and password, then try again.'
  }

  if (error.status === 401) {
    return 'The login identifier or password is incorrect.'
  }

  if (error.status === 403) {
    return securityMessage
  }

  if (error.status === 429) {
    return 'Too many sign-in attempts. Wait a few minutes before trying again.'
  }

  if (typeof error.status === 'number' && error.status >= 500) {
    return serverMessage
  }

  return unexpectedMessage
}

export function getLogoutErrorMessage(error) {
  if (!error) {
    return 'Sign out did not complete. Try again.'
  }

  if (error.status === 'FETCH_ERROR') {
    return networkMessage
  }

  if (error.status === 'TIMEOUT_ERROR') {
    return timeoutMessage
  }

  if (error.status === 403) {
    return securityMessage
  }

  if (typeof error.status === 'number' && error.status >= 500) {
    return serverMessage
  }

  return 'Sign out could not be completed. Try again.'
}
