import React from 'react'
import { buttonStyles } from './buttonStyles.js'

export function Button({
  variant = 'primary',
  size = 'md',
  className,
  type = 'button',
  as: Component = 'button',
  ...props
}) {
  const sharedProps = {
    className: buttonStyles({ variant, size, className }),
    ...props,
  }

  if (Component === 'button') {
    return <button type={type} {...sharedProps} />
  }

  return React.createElement(Component, sharedProps)
}