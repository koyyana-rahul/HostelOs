import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { User, Briefcase, ShieldCheck, LogOut } from 'lucide-react'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { isCustomerOnly } from '../../auth/lib/roleResolver.js'
import { Button } from '../../../components/ui/Button.jsx'
import { useLogout } from '../../auth/hooks/useLogout.js'

export function CustomerAccountMenu() {
  const [open, setOpen] = useState(false)
  const user = useAppSelector(selectCurrentUser)
  const navigate = useNavigate()
  const { signOut, isSigningOut } = useLogout()

  if (!isCustomerOnly(user)) {
    return null
  }

  const goTo = (path) => {
    setOpen(false)
    navigate(path)
  }

  return (
    <div className="relative z-10">
      <Button
        onClick={() => setOpen((value) => !value)}
        variant="ghost"
        size="sm"
        aria-label="Account menu"
        aria-expanded={open}
      >
        <User className="size-4" aria-hidden="true" />
        My account
      </Button>

      {open ? (
        <div className="absolute right-0 mt-2 flex w-52 flex-col rounded-xl border border-line bg-surface p-2 shadow-lg">
          <Button
            onClick={() => goTo('/account/profile')}
            variant="ghost"
            size="sm"
            className="justify-start"
          >
            <User className="size-4" aria-hidden="true" />
            My profile
          </Button>
          <Button
            onClick={() => goTo('/my-enquiries')}
            variant="ghost"
            size="sm"
            className="justify-start"
          >
            <Briefcase className="size-4" aria-hidden="true" />
            My enquiries
          </Button>
          <Button
            onClick={() => goTo('/account/settings')}
            variant="ghost"
            size="sm"
            className="justify-start"
          >
            <ShieldCheck className="size-4" aria-hidden="true" />
            Account settings
          </Button>
          <Button
            onClick={() => {
              setOpen(false)
              signOut()
            }}
            disabled={isSigningOut}
            variant="ghost"
            size="sm"
            className="justify-start text-danger"
          >
            <LogOut className="size-4" aria-hidden="true" />
            {isSigningOut ? 'Signing out...' : 'Sign out'}
          </Button>
        </div>
      ) : null}
    </div>
  )
}
