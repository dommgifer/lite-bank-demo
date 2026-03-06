import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { accountAPI, exchangeAPI } from '../services/api'
import { startSpan, endSpan, setSpanAttributes } from '../tracing'
import { formatCurrency } from '../utils/formatCurrency'
import {
  ArrowsUpDownIcon,
  ArrowPathIcon,
  ChartBarIcon
} from '@heroicons/react/24/outline'

export default function Exchange() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [step, setStep] = useState(1) // 1: Exchange, 2: Complete, 3: Failed
  const [accounts, setAccounts] = useState([])
  const [fromCurrency, setFromCurrency] = useState('USD')
  const [toCurrency, setToCurrency] = useState('EUR')
  const [amount, setAmount] = useState('1000')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState(null)
  const [exchangeResult, setExchangeResult] = useState(null)
  const [failedTransaction, setFailedTransaction] = useState(null) // 失敗交易資訊

  // 解析錯誤類型
  const getErrorType = (error) => {
    const errorType = error.response?.data?.error?.type
    if (errorType) return errorType
    // 從錯誤訊息中推斷類型
    const message = error.response?.data?.error?.message || error.message || ''
    if (message.toLowerCase().includes('insufficient')) return 'INSUFFICIENT_BALANCE'
    if (message.toLowerCase().includes('not found')) return 'ACCOUNT_NOT_FOUND'
    if (message.toLowerCase().includes('mismatch')) return 'CURRENCY_MISMATCH'
    return 'UNKNOWN'
  }

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

    // 找到對應幣別的帳戶
    const sourceAccount = accounts.find(acc => acc.currency === fromCurrency)
    const destinationAccount = accounts.find(acc => acc.currency === toCurrency)

    if (!sourceAccount) {
      setMessage({ type: 'error', text: `No ${fromCurrency} account found` })
      setSubmitting(false)
      return
    }

    if (!destinationAccount) {
      setMessage({ type: 'error', text: `No ${toCurrency} account found` })
      setSubmitting(false)
      return
    }

    // 保存原始資料
    const originalFromCurrency = fromCurrency
    const originalToCurrency = toCurrency
    const originalAmount = parseFloat(amount.replace(/,/g, ''))
    const originalRate = getRate()
    const originalConvertedAmount = getConvertedAmount()

    // 建立換匯追蹤 span
    const span = startSpan('exchange.submit')
    setSpanAttributes(span, {
      'exchange.from_currency': fromCurrency,
      'exchange.to_currency': toCurrency,
      'exchange.amount': originalAmount,
      'exchange.rate': originalRate,
      'exchange.converted_amount': originalConvertedAmount,
    })

    try {
      const response = await exchangeAPI.exchange({
        sourceAccountId: sourceAccount.accountId,
        destinationAccountId: destinationAccount.accountId,
        sourceCurrency: fromCurrency,
        destinationCurrency: toCurrency,
        amount: originalAmount,
      })

      // 記錄成功屬性
      setSpanAttributes(span, {
        'exchange.id': response.data.data?.exchangeId || response.data.data?.id,
        'exchange.status': response.data.data?.status || 'COMPLETED',
      })
      endSpan(span, 'OK')

      // 設定換匯結果
      setExchangeResult({
        ...response.data.data,
        fromCurrency: originalFromCurrency,
        toCurrency: originalToCurrency,
        fromAmount: originalAmount,
        toAmount: originalConvertedAmount,
        rate: originalRate,
        sourceAccount: sourceAccount,
        destinationAccount: destinationAccount,
      })
      setStep(2)
      loadAccounts()
      window.scrollTo(0, 0)
    } catch (error) {
      const errorType = getErrorType(error)
      setSpanAttributes(span, {
        'error.type': errorType,
        'error.message': error.response?.data?.error?.message || error.message,
      })
      endSpan(span, 'ERROR', error.response?.data?.error?.message || 'Exchange failed')

      // 設定失敗交易資訊並導向失敗頁面
      setFailedTransaction({
        errorType,
        fromCurrency: originalFromCurrency,
        toCurrency: originalToCurrency,
        fromAmount: originalAmount,
        toAmount: originalConvertedAmount,
        rate: originalRate,
        sourceAccount: sourceAccount,
        destinationAccount: destinationAccount,
        timestamp: new Date(),
      })
      setStep(3) // 導向失敗頁面
      window.scrollTo(0, 0)
    } finally {
      setSubmitting(false)
    }
  }

  const handleNewExchange = () => {
    setStep(1)
    setAmount('1000')
    setMessage(null)
    setExchangeResult(null)
    setFailedTransaction(null)
    window.scrollTo(0, 0)
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

  // 從用戶帳戶中提取可用幣別
  const userCurrencies = accounts.map(acc => acc.currency)

  const getAccountBalance = (currency) => {
    const account = accounts.find(a => a.currency === currency)
    return account?.balance || 0
  }

  // 當帳戶載入後，設定有效的預設幣別
  useEffect(() => {
    if (accounts.length > 0) {
      const currencies = accounts.map(acc => acc.currency)

      // 如果當前 fromCurrency 不在用戶帳戶中，改成第一個
      if (!currencies.includes(fromCurrency)) {
        setFromCurrency(currencies[0])
      }

      // 如果當前 toCurrency 不在用戶帳戶中，改成第二個（若有）
      if (!currencies.includes(toCurrency)) {
        const available = currencies.filter(c => c !== fromCurrency)
        if (available.length > 0) {
          setToCurrency(available[0])
        }
      }
    }
  }, [accounts])

  // 當 fromCurrency 改變時，確保 toCurrency 不重複
  useEffect(() => {
    if (accounts.length > 0 && fromCurrency === toCurrency) {
      const available = userCurrencies.filter(c => c !== fromCurrency)
      if (available.length > 0) {
        setToCurrency(available[0])
      }
    }
  }, [fromCurrency])

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
        <h1 className="text-3xl font-heading font-bold text-text mb-2">{t('exchange.title')}</h1>
        <p className="text-text/60">{t('exchange.subtitle')}</p>
      </div>

      {/* Step 3: Exchange Failed */}
      {step === 3 && failedTransaction && (
        <div className="max-w-2xl mx-auto">
          <div className="bg-white rounded-2xl shadow-sm border border-border p-6">
            {/* Failed Message */}
            <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center mb-6">
              <div className="w-16 h-16 bg-red-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>
              <h3 className="text-2xl font-heading font-bold text-red-700 mb-2">{t('errors.exchangeFailed')}</h3>
              <p className="text-red-600">{t('errors.cannotComplete')}</p>
            </div>

            {/* Error Reason */}
            <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-6">
              <h4 className="font-semibold text-red-700 mb-2">{t('errors.failureReason')}</h4>
              <p className="text-lg font-medium text-red-800">
                {t(`errors.types.${failedTransaction.errorType}.title`)}
              </p>
              <p className="text-red-600 text-sm mt-1">
                {t(`errors.types.${failedTransaction.errorType}.description`)}
              </p>
            </div>

            {/* Transaction Summary */}
            <div className="bg-surface rounded-xl p-6 space-y-4 mb-6">
              <h4 className="font-semibold text-text">{t('errors.transactionSummary')}</h4>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.fromAccount')}</span>
                <span className="font-medium font-mono text-text">{failedTransaction.sourceAccount?.accountNumber}</span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.toAccount')}</span>
                <span className="font-medium font-mono text-text">{failedTransaction.destinationAccount?.accountNumber}</span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.exchangeAmount')}</span>
                <span className="text-xl font-bold text-text">
                  {formatCurrency(failedTransaction.fromAmount)} {failedTransaction.fromCurrency}
                </span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.youReceive')}</span>
                <span className="font-medium text-text">
                  {formatCurrency(failedTransaction.toAmount)} {failedTransaction.toCurrency}
                </span>
              </div>

              <div className="flex justify-between items-center py-2">
                <span className="text-text/60">{t('exchange.dateTime')}</span>
                <span className="text-text">{failedTransaction.timestamp.toLocaleString()}</span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-4">
              <button
                type="button"
                onClick={handleNewExchange}
                className="flex-1 py-4 bg-surface border border-red-300 text-red-600 rounded-xl font-semibold hover:bg-red-50 transition-colors duration-200 cursor-pointer"
              >
                {t('errors.tryAgain')}
              </button>
              <button
                type="button"
                onClick={() => window.location.href = '/accounts'}
                className="flex-1 py-4 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer"
              >
                {t('errors.viewMyAccounts')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Step 2: Exchange Complete */}
      {step === 2 && exchangeResult && (
        <div className="max-w-2xl mx-auto">
          <div className="bg-white rounded-2xl shadow-sm border border-border p-6">
            {/* Success Message */}
            <div className="bg-green-50 border border-green-200 rounded-xl p-6 text-center mb-6">
              <div className="w-16 h-16 bg-green-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h3 className="text-2xl font-heading font-bold text-green-700 mb-2">{t('exchange.success')}</h3>
              <p className="text-green-600">{t('exchange.successMessage')}</p>
            </div>

            {/* Exchange Details */}
            <div className="bg-surface rounded-xl p-6 space-y-4 mb-6">
              <h4 className="font-semibold text-text">{t('exchange.exchangeDetails')}</h4>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.transactionId')}</span>
                <span className="font-mono text-sm text-text">#{exchangeResult.exchangeId || exchangeResult.id || '-'}</span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.fromAccount')}</span>
                <span className="font-medium font-mono text-text">{exchangeResult.sourceAccount?.accountNumber}</span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.toAccount')}</span>
                <span className="font-medium font-mono text-text">{exchangeResult.destinationAccount?.accountNumber}</span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.youPaid')}</span>
                <span className="text-xl font-bold text-red-500">
                  -{formatCurrency(exchangeResult.fromAmount)} {exchangeResult.fromCurrency}
                </span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.youReceived')}</span>
                <span className="text-xl font-bold text-green-600">
                  +{formatCurrency(exchangeResult.toAmount)} {exchangeResult.toCurrency}
                </span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.exchangeRate')}</span>
                <span className="font-medium text-text">
                  1 {exchangeResult.fromCurrency} = {exchangeResult.rate?.toFixed(4)} {exchangeResult.toCurrency}
                </span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.exchangeFee')}</span>
                <span className="text-green-600 font-medium">{t('common.free')}</span>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-border">
                <span className="text-text/60">{t('exchange.dateTime')}</span>
                <span className="text-text">{new Date(exchangeResult.createdAt || Date.now()).toLocaleString()}</span>
              </div>

              <div className="flex justify-between items-center py-2">
                <span className="text-text/60">{t('common.status')}</span>
                <span className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">
                  {exchangeResult.status || 'COMPLETED'}
                </span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => window.location.href = '/history'}
                className="flex-1 py-4 bg-surface border border-border text-text rounded-xl font-semibold hover:bg-border/50 transition-colors duration-200 cursor-pointer"
              >
                {t('exchange.viewHistory')}
              </button>
              <button
                type="button"
                onClick={handleNewExchange}
                className="flex-1 py-4 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer"
              >
                {t('exchange.newExchange')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Step 1: Exchange Form */}
      {step === 1 && userCurrencies.length < 2 && (
        <div className="max-w-2xl mx-auto">
          <div className="bg-white rounded-2xl shadow-sm border border-border p-6">
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-6 text-center">
              <div className="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg className="w-8 h-8 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <h3 className="text-xl font-heading font-bold text-amber-700 mb-2">
                {t('exchange.needMultipleCurrencies')}
              </h3>
              <p className="text-amber-600 mb-6">
                {t('exchange.needMultipleCurrenciesDesc')}
              </p>
              <button
                type="button"
                onClick={() => window.location.href = '/accounts'}
                className="px-6 py-3 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer"
              >
                {t('exchange.goToAccounts')}
              </button>
            </div>
          </div>
        </div>
      )}

      {step === 1 && userCurrencies.length >= 2 && (
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
                <label className="block text-sm font-medium text-text mb-2">{t('exchange.youPay')}</label>
                <div className="flex items-center space-x-4 p-4 bg-surface rounded-xl border border-border">
                  <div className="flex-1">
                    <input
                      type="text"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value.replace(/[^0-9.,]/g, ''))}
                      className="w-full text-3xl font-bold text-text bg-transparent focus:outline-none"
                    />
                    <p className="text-sm text-text/50 mt-1">
                      {t('exchange.available')}: {formatCurrency(getAccountBalance(fromCurrency))} {fromCurrency}
                    </p>
                  </div>
                  <select
                    value={fromCurrency}
                    onChange={(e) => setFromCurrency(e.target.value)}
                    className="flex items-center space-x-2 px-4 py-3 bg-white rounded-xl border border-border cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/20"
                  >
                    {userCurrencies.map((c) => (
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
                <label className="block text-sm font-medium text-text mb-2">{t('exchange.youReceive')}</label>
                <div className="flex items-center space-x-4 p-4 bg-primary/5 rounded-xl border border-primary/20">
                  <div className="flex-1">
                    <p className="text-3xl font-bold text-primary">
                      {formatCurrency(getConvertedAmount())}
                    </p>
                    <p className="text-sm text-text/50 mt-1">
                      {t('exchange.balanceAfter')}: {formatCurrency(getAccountBalance(toCurrency) + getConvertedAmount())} {toCurrency}
                    </p>
                  </div>
                  <select
                    value={toCurrency}
                    onChange={(e) => setToCurrency(e.target.value)}
                    className="flex items-center space-x-2 px-4 py-3 bg-white rounded-xl border border-border cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/20"
                  >
                    {userCurrencies.filter(c => c !== fromCurrency).map((c) => (
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
                    <span className="font-medium text-text">{t('exchange.exchangeRate')}</span>
                  </div>
                  <span className="text-xs text-text/50">{t('exchange.updatedSecondsAgo', { seconds: 5 })}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-2xl font-bold text-text">
                      1 {fromCurrency} = {getRate().toFixed(4)} {toCurrency}
                    </p>
                    <p className="text-sm text-green-600">{t('exchange.fromYesterday', { change: '+0.12%' })}</p>
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
                  <span className="text-text/60">{t('exchange.exchangeAmount')}</span>
                  <span className="text-text">{formatCurrency(parseFloat(amount.replace(/,/g, '')) || 0)} {fromCurrency}</span>
                </div>
                <div className="flex justify-between mb-2">
                  <span className="text-text/60">{t('exchange.exchangeFee')}</span>
                  <span className="text-green-600 font-medium">{t('common.free')}</span>
                </div>
                <div className="flex justify-between pt-2 border-t border-border">
                  <span className="font-semibold text-text">{t('exchange.total')}</span>
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
                    <span>{t('exchange.exchangeNow')}</span>
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
                <h3 className="font-heading font-semibold text-text">{t('exchange.liveRates')}</h3>
                <span className="text-xs text-green-600 bg-green-100 px-2 py-1 rounded-full flex items-center">
                  <span className="w-1.5 h-1.5 bg-green-600 rounded-full mr-1 animate-pulse"></span>
                  {t('exchange.live')}
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
              <h3 className="font-heading font-semibold text-text mb-4">{t('exchange.recentExchanges')}</h3>
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
      )}
    </div>
  )
}
