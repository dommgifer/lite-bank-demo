import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import {
  BellIcon,
  CurrencyDollarIcon
} from '@heroicons/react/24/outline'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navLinks = [
    { to: '/', label: 'Dashboard' },
    { to: '/accounts', label: 'Accounts' },
    { to: '/transfer', label: 'Transfer' },
    { to: '/exchange', label: 'Exchange' },
    { to: '/history', label: 'History' },
  ]

  return (
    <nav className="fixed top-4 left-4 right-4 bg-white/90 backdrop-blur-sm rounded-2xl shadow-lg border border-border z-50">
      <div className="max-w-7xl mx-auto px-6 py-4">
        <div className="flex items-center justify-between">
          {/* Logo */}
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 bg-primary rounded-xl flex items-center justify-center">
              <CurrencyDollarIcon className="w-6 h-6 text-white" />
            </div>
            <span className="text-xl font-heading font-semibold text-text">Lite Bank</span>
          </div>

          {/* Navigation Links */}
          <div className="hidden md:flex items-center space-x-1">
            {navLinks.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `px-4 py-2 rounded-xl transition-colors duration-200 cursor-pointer ${
                    isActive
                      ? 'bg-primary/10 text-primary font-medium'
                      : 'text-text/70 hover:bg-primary/5 hover:text-primary'
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </div>

          {/* User Profile */}
          <div className="flex items-center space-x-4">
            <button className="p-2 rounded-xl hover:bg-primary/5 transition-colors duration-200 cursor-pointer">
              <BellIcon className="w-6 h-6 text-text/70" />
            </button>
            <div
              onClick={handleLogout}
              className="w-10 h-10 bg-secondary rounded-full flex items-center justify-center cursor-pointer hover:opacity-80 transition-opacity"
              title="Click to logout"
            >
              <span className="text-white font-semibold">
                {user?.username?.charAt(0).toUpperCase() || 'U'}
              </span>
            </div>
          </div>
        </div>
      </div>
    </nav>
  )
}
