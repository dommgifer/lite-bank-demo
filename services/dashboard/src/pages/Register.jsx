import { useState } from 'react'
import { useNavigate, Navigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { authAPI } from '../services/api'
import {
  CurrencyDollarIcon,
  UserIcon,
  LockClosedIcon,
  EnvelopeIcon,
  EyeIcon,
  EyeSlashIcon,
  CheckIcon,
  ArrowRightIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline'

export default function Register() {
  const { t } = useTranslation()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)

  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setFieldErrors({})
    setLoading(true)

    try {
      await authAPI.register(username, email, password)
      setSuccess(true)
      setTimeout(() => navigate('/login'), 2000)
    } catch (err) {
      const data = err.response?.data
      if (data?.error?.details) {
        setFieldErrors(data.error.details)
      } else {
        setError(data?.error?.message || t('auth.registerFailed'))
      }
    } finally {
      setLoading(false)
    }
  }

  const features = [
    t('auth.features.multiCurrency'),
    t('auth.features.realTimeRates'),
    t('auth.features.instantTransfers'),
    t('auth.features.bankSecurity'),
  ]

  return (
    <div className="min-h-screen bg-surface flex">
      {/* Left Panel - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary to-secondary p-12 flex-col justify-between">
        <div>
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
              <CurrencyDollarIcon className="w-7 h-7 text-white" />
            </div>
            <span className="text-2xl font-heading font-bold text-white">Lite Bank</span>
          </div>
        </div>

        <div className="space-y-6">
          <h1 className="text-4xl font-heading font-bold text-white leading-tight">
            {t('auth.bankingSlogan')}
          </h1>
          <p className="text-white/80 text-lg max-w-md">
            {t('auth.bankingDescription')}
          </p>

          <div className="space-y-4 pt-4">
            {features.map((feature) => (
              <div key={feature} className="flex items-center space-x-3">
                <div className="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
                  <CheckIcon className="w-4 h-4 text-white" />
                </div>
                <span className="text-white/90">{feature}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="text-white/60 text-sm">
          <p>{t('auth.copyright')}</p>
        </div>
      </div>

      {/* Right Panel - Register Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden flex items-center justify-center space-x-3 mb-8">
            <div className="w-12 h-12 bg-primary rounded-xl flex items-center justify-center">
              <CurrencyDollarIcon className="w-7 h-7 text-white" />
            </div>
            <span className="text-2xl font-heading font-bold text-text">Lite Bank</span>
          </div>

          {/* Register Card */}
          <div className="bg-white rounded-2xl shadow-lg border border-border p-8">
            <div className="text-center mb-8">
              <h2 className="text-2xl font-heading font-bold text-text mb-2">{t('auth.createAccount')}</h2>
              <p className="text-text/60">{t('auth.createAccountSubtitle')}</p>
            </div>

            {/* Success State */}
            {success && (
              <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-xl flex items-center space-x-3">
                <CheckCircleIcon className="w-5 h-5 text-green-500 shrink-0" />
                <div>
                  <p className="text-green-700 font-medium text-sm">{t('auth.registerSuccess')}</p>
                  <p className="text-green-600 text-sm">{t('auth.registerSuccessDesc')}</p>
                </div>
              </div>
            )}

            {/* Error */}
            {error && (
              <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              {/* Username */}
              <div>
                <label className="block text-sm font-medium text-text mb-2">{t('auth.username')}</label>
                <div className="relative">
                  <UserIcon className="w-5 h-5 text-text/40 absolute left-4 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder={t('auth.enterUsername')}
                    className={`w-full pl-12 pr-4 py-3 bg-surface border rounded-xl text-text placeholder-text/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200 ${fieldErrors.username ? 'border-red-400' : 'border-border'}`}
                    required
                  />
                </div>
                {fieldErrors.username ? (
                  <p className="mt-1 text-xs text-red-500">{fieldErrors.username}</p>
                ) : (
                  <p className="mt-1 text-xs text-text/40">{t('auth.usernameHint')}</p>
                )}
              </div>

              {/* Email */}
              <div>
                <label className="block text-sm font-medium text-text mb-2">{t('auth.email')}</label>
                <div className="relative">
                  <EnvelopeIcon className="w-5 h-5 text-text/40 absolute left-4 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder={t('auth.enterEmail')}
                    className={`w-full pl-12 pr-4 py-3 bg-surface border rounded-xl text-text placeholder-text/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200 ${fieldErrors.email ? 'border-red-400' : 'border-border'}`}
                    required
                  />
                </div>
                {fieldErrors.email && (
                  <p className="mt-1 text-xs text-red-500">{fieldErrors.email}</p>
                )}
              </div>

              {/* Password */}
              <div>
                <label className="block text-sm font-medium text-text mb-2">{t('auth.password')}</label>
                <div className="relative">
                  <LockClosedIcon className="w-5 h-5 text-text/40 absolute left-4 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder={t('auth.enterPassword')}
                    className={`w-full pl-12 pr-12 py-3 bg-surface border rounded-xl text-text placeholder-text/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200 ${fieldErrors.password ? 'border-red-400' : 'border-border'}`}
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-text/40 hover:text-text/60 cursor-pointer"
                  >
                    {showPassword ? (
                      <EyeSlashIcon className="w-5 h-5" />
                    ) : (
                      <EyeIcon className="w-5 h-5" />
                    )}
                  </button>
                </div>
                {fieldErrors.password ? (
                  <p className="mt-1 text-xs text-red-500">{fieldErrors.password}</p>
                ) : (
                  <p className="mt-1 text-xs text-text/40">{t('auth.passwordHint')}</p>
                )}
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={loading || success}
                className="w-full py-3 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                ) : (
                  <>
                    <span>{t('auth.createAccount')}</span>
                    <ArrowRightIcon className="w-5 h-5" />
                  </>
                )}
              </button>
            </form>

            <p className="text-center mt-6 text-text/60">
              {t('auth.alreadyHaveAccount')}{' '}
              <Link to="/login" className="text-primary font-medium hover:text-primary/80 cursor-pointer">
                {t('auth.signIn')}
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
