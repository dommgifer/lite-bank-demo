import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { accountAPI, exchangeAPI } from '../services/api'
import {
  ArrowsUpDownIcon,
  ArrowPathIcon,
  ChartBarIcon
} from '@heroicons/react/24/outline'

export default function Exchange() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [fromCurrency, setFromCurrency] = useState('USD')
  const [toCurrency, setToCurrency] = useState('EUR')
  const [amount, setAmount] = useState('1000')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState(null)

  // Mock exchange rates
  const exchangeRates = {
    'USD-EUR': 0.92,
    'USD-TWD': 31.25,
    'USD-JPY': 148.50,
    'EUR-USD': 1.087,
    'EUR-TWD': 33.85,
    'EUR-JPY': 161.50,
    'TWD-USD': 0.032,
    'TWD-EUR': 0.0295,
    'JPY-USD': 0.00673,
    'JPY-EUR': 0.0062,
  }

  useEffect(() => {
    loadAccounts()
  }, [user])

  const loadAccounts = async () => {
    try {
      const res = await accountAPI.getByUserId(user?.id || 1)
      setAccounts(res.data.data || [])
    } catch (error) {
      console.error('Failed to load accounts:', error)
    } finally {
      setLoading(false)
    }
  }

  const getRate = () => {
    if (fromCurrency === toCurrency) return 1
    return exchangeRates[`${fromCurrency}-${toCurrency}`] || 1
  }

  const getConvertedAmount = () => {
    const numAmount = parseFloat(amount.replace(/,/g, '')) || 0
    return numAmount * getRate()
  }

  const handleSwap = () => {
    setFromCurrency(toCurrency)
    setToCurrency(fromCurrency)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setMessage(null)

    try {
      await exchangeAPI.exchange({
        fromCurrency,
        toCurrency,
        amount: parseFloat(amount.replace(/,/g, '')),
      })
      setMessage({ type: 'success', text: 'Exchange successful!' })
      loadAccounts()
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.message || 'Exchange failed'
      })
    } finally {
      setSubmitting(false)
    }
  }

  const getCurrencyIcon = (currency) => {
    const icons = {
      USD: { symbol: '$', bg: 'bg-cta/10', text: 'text-cta' },
      EUR: { symbol: '€', bg: 'bg-purple-100', text: 'text-purple-600' },
      TWD: { symbol: 'NT', bg: 'bg-orange-100', text: 'text-orange-600' },
      JPY: { symbol: '¥', bg: 'bg-red-100', text: 'text-red-600' },
    }
    return icons[currency] || icons.USD
  }

  const formatCurrency = (amount, currency = 'USD') => {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount)
  }

  const currencies = ['USD', 'EUR', 'TWD', 'JPY']
  const getAccountBalance = (currency) => {
    const account = accounts.find(a => a.currency === currency)
    return account?.balance || 0
  }

  const liveRates = [
    { pair: 'USD/EUR', rate: '0.9200', change: '+0.12%', positive: true },
    { pair: 'USD/TWD', rate: '31.25', change: '-0.08%', positive: false },
    { pair: 'USD/JPY', rate: '148.50', change: '+0.23%', positive: true },
    { pair: 'EUR/TWD', rate: '33.85', change: '+0.05%', positive: true },
  ]

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    )
  }

  return (
    <div>
      {/* Page Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-heading font-bold text-text mb-2">Currency Exchange</h1>
        <p className="text-text/60">Exchange currencies at competitive rates</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Exchange Calculator */}
        <div className="lg:col-span-2">
          <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-border p-6">
            {message && (
              <div className={`mb-6 p-4 rounded-xl ${
                message.type === 'success'
                  ? 'bg-green-50 border border-green-200 text-green-600'
                  : 'bg-red-50 border border-red-200 text-red-600'
              }`}>
                {message.text}
              </div>
            )}

            {/* From Currency */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-text mb-2">You Pay</label>
              <div className="flex items-center space-x-4 p-4 bg-surface rounded-xl border border-border">
                <div className="flex-1">
                  <input
                    type="text"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value.replace(/[^0-9.,]/g, ''))}
                    className="w-full text-3xl font-bold text-text bg-transparent focus:outline-none"
                  />
                  <p className="text-sm text-text/50 mt-1">
                    Available: {formatCurrency(getAccountBalance(fromCurrency))} {fromCurrency}
                  </p>
                </div>
                <select
                  value={fromCurrency}
                  onChange={(e) => setFromCurrency(e.target.value)}
                  className="flex items-center space-x-2 px-4 py-3 bg-white rounded-xl border border-border cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/20"
                >
                  {currencies.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Swap Button */}
            <div className="flex justify-center -my-2 relative z-10">
              <button
                type="button"
                onClick={handleSwap}
                className="w-12 h-12 bg-primary text-white rounded-full flex items-center justify-center shadow-lg hover:bg-primary/90 transition-colors duration-200 cursor-pointer"
              >
                <ArrowsUpDownIcon className="w-5 h-5" />
              </button>
            </div>

            {/* To Currency */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-text mb-2">You Receive</label>
              <div className="flex items-center space-x-4 p-4 bg-primary/5 rounded-xl border border-primary/20">
                <div className="flex-1">
                  <p className="text-3xl font-bold text-primary">
                    {formatCurrency(getConvertedAmount())}
                  </p>
                  <p className="text-sm text-text/50 mt-1">
                    Balance after: {formatCurrency(getAccountBalance(toCurrency) + getConvertedAmount())} {toCurrency}
                  </p>
                </div>
                <select
                  value={toCurrency}
                  onChange={(e) => setToCurrency(e.target.value)}
                  className="flex items-center space-x-2 px-4 py-3 bg-white rounded-xl border border-border cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/20"
                >
                  {currencies.filter(c => c !== fromCurrency).map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Exchange Rate Info */}
            <div className="bg-surface rounded-xl p-4 mb-6">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center space-x-2">
                  <ChartBarIcon className="w-5 h-5 text-primary" />
                  <span className="font-medium text-text">Exchange Rate</span>
                </div>
                <span className="text-xs text-text/50">Updated 5 sec ago</span>
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-2xl font-bold text-text">
                    1 {fromCurrency} = {getRate().toFixed(4)} {toCurrency}
                  </p>
                  <p className="text-sm text-green-600">+0.12% from yesterday</p>
                </div>
                <button
                  type="button"
                  className="p-2 rounded-lg hover:bg-primary/10 transition-colors duration-200 cursor-pointer"
                >
                  <ArrowPathIcon className="w-5 h-5 text-primary" />
                </button>
              </div>
            </div>

            {/* Fee Breakdown */}
            <div className="border-t border-border pt-4 mb-6">
              <div className="flex justify-between mb-2">
                <span className="text-text/60">Exchange Amount</span>
                <span className="text-text">{formatCurrency(parseFloat(amount.replace(/,/g, '')) || 0)} {fromCurrency}</span>
              </div>
              <div className="flex justify-between mb-2">
                <span className="text-text/60">Exchange Fee</span>
                <span className="text-green-600 font-medium">Free</span>
              </div>
              <div className="flex justify-between pt-2 border-t border-border">
                <span className="font-semibold text-text">Total</span>
                <span className="font-bold text-text text-lg">{formatCurrency(parseFloat(amount.replace(/,/g, '')) || 0)} {fromCurrency}</span>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={submitting}
              className="w-full py-4 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? (
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
              ) : (
                <>
                  <ArrowsUpDownIcon className="w-5 h-5" />
                  <span>Exchange Now</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Live Rates */}
          <div className="bg-white rounded-2xl shadow-sm border border-border p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-heading font-semibold text-text">Live Rates</h3>
              <span className="text-xs text-green-600 bg-green-100 px-2 py-1 rounded-full flex items-center">
                <span className="w-1.5 h-1.5 bg-green-600 rounded-full mr-1 animate-pulse"></span>
                Live
              </span>
            </div>

            <div className="space-y-3">
              {liveRates.map((item) => (
                <div
                  key={item.pair}
                  className="flex items-center justify-between p-3 rounded-xl bg-surface hover:bg-primary/5 transition-colors duration-200 cursor-pointer"
                >
                  <div className="flex items-center space-x-3">
                    <span className="text-sm font-medium text-text">{item.pair}</span>
                  </div>
                  <div className="text-right">
                    <p className="font-semibold text-text">{item.rate}</p>
                    <p className={`text-xs ${item.positive ? 'text-green-600' : 'text-red-500'}`}>
                      {item.change}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Recent Exchanges */}
          <div className="bg-white rounded-2xl shadow-sm border border-border p-6">
            <h3 className="font-heading font-semibold text-text mb-4">Recent Exchanges</h3>
            <div className="space-y-3">
              <div className="p-3 rounded-xl bg-surface">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-text">USD → EUR</span>
                  <span className="text-xs text-text/50">Jan 3</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-red-500">-$1,000.00</span>
                  <span className="text-text/30">→</span>
                  <span className="text-green-600">+€918.50</span>
                </div>
              </div>
              <div className="p-3 rounded-xl bg-surface">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-text">TWD → USD</span>
                  <span className="text-xs text-text/50">Dec 28</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-red-500">-NT$31,000</span>
                  <span className="text-text/30">→</span>
                  <span className="text-green-600">+$1,000.00</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
