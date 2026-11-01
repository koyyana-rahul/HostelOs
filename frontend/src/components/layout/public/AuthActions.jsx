import { Fragment } from 'react'
import { Link } from 'react-router-dom'
import { buttonStyles } from '../ui/buttonStyles.js'

export function AuthActions({ onNavigate }) {
  return (
    <Fragment>
      <Link
        to="/login"
        onClick={onNavigate}
        className={buttonStyles({ variant: 'ghost' })}
      >
        Sign in
      </Link>
      <Link
        to="/register"
        onClick={onNavigate}
        className={buttonStyles({ variant: 'primary' })}
      >
        Get started
      </Link>
    </Fragment>
  )
}