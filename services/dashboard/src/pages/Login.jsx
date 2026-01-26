import { useState } from 'react'
import { useNavigate, Navigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { startSpan, endSpan, setSpanAttributes } from '../tracing'
import {
  CurrencyDollarIcon,
  UserIcon,
  LockClosedIcon,
  EyeIcon,
  EyeSlashIcon,
  CheckIcon,
  ArrowRightIcon
} from '@heroicons/react/24/outline'

export default function Login() {
  const { t } = useTranslation()
  const [username, setUsername] = useState('alice')
  const [password, setPassword] = useState('password123')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(true)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    // 建立登入追蹤 span
    const span = startSpan('auth.login')
    setSpanAttributes(span, { 'user.username': username })

    const result = await login(username, password)

    if (result.success) {
      endSpan(span, 'OK')
      navigate('/')
    } else {
      setSpanAttributes(span, { 'error.reason': result.error })
      endSpan(span, 'ERROR', result.error)
      setError(result.error)
    }

    setLoading(false)
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

      {/* Right Panel - Login Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden flex items-center justify-center space-x-3 mb-8">
            <div className="w-12 h-12 bg-primary rounded-xl flex items-center justify-center">
              <CurrencyDollarIcon className="w-7 h-7 text-white" />
            </div>
            <span className="text-2xl font-heading font-bold text-text">Lite Bank</span>
          </div>

          {/* Login Card */}
          <div className="bg-white rounded-2xl shadow-lg border border-border p-8">
            <div className="text-center mb-8">
              <h2 className="text-2xl font-heading font-bold text-text mb-2">{t('auth.welcomeBack')}</h2>
              <p className="text-text/60">{t('auth.signInToContinue')}</p>
            </div>

            {error && (
              <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
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
                    className="w-full pl-12 pr-4 py-3 bg-surface border border-border rounded-xl text-text placeholder-text/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200"
                    required
                  />
                </div>
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
                    className="w-full pl-12 pr-12 py-3 bg-surface border border-border rounded-xl text-text placeholder-text/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200"
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
              </div>

              {/* Remember & Forgot */}
              <div className="flex items-center justify-between">
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="w-4 h-4 rounded border-border text-primary focus:ring-primary/20 cursor-pointer"
                  />
                  <span className="text-sm text-text/70">{t('auth.rememberMe')}</span>
                </label>
                <a href="#" className="text-sm text-primary hover:text-primary/80 cursor-pointer">
                  {t('auth.forgotPassword')}
                </a>
              </div>

              {/* Login Button */}
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                ) : (
                  <>
                    <span>{t('auth.signIn')}</span>
                    <ArrowRightIcon className="w-5 h-5" />
                  </>
                )}
              </button>
            </form>

            <p className="text-center mt-6 text-text/60">
              {t('auth.noAccount')}{' '}
              <a href="#" className="text-primary font-medium hover:text-primary/80 cursor-pointer">
                {t('auth.signUp')}
              </a>
            </p>
          </div>

          {/* Demo Credentials */}
          <div className="mt-6 p-4 bg-primary/5 rounded-xl border border-primary/20">
            <p className="text-sm text-primary font-medium mb-2">{t('auth.demoCredentials')}</p>
            <div className="text-sm text-text/70 space-y-1">
              <p><span className="text-text/50">Username:</span> alice</p>
              <p><span className="text-text/50">Password:</span> password123</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
