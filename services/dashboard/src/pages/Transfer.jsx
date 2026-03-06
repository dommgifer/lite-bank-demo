import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { accountAPI, transferAPI, recipientAPI } from '../services/api'
import { startSpan, endSpan, setSpanAttributes, maskAccount } from '../tracing'
import { formatCurrency } from '../utils/formatCurrency'
import { ArrowDownIcon, ArrowRightIcon, UserIcon } from '@heroicons/react/24/outline'

export default function Transfer() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [step, setStep] = useState(1) // 1: Details, 2: Confirm, 3: Complete, 4: Failed
  const [accounts, setAccounts] = useState([])
  const [recipients, setRecipients] = useState([])
  const [fromAccount, setFromAccount] = useState(null)
  const [toAccount, setToAccount] = useState(null)
  const [toAccountNumber, setToAccountNumber] = useState('')
  const [lookingUpAccount, setLookingUpAccount] = useState(false)
  const [amount, setAmount] = useState('1000')
  const [note, setNote] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState(null)
  const [transferResult, setTransferResult] = useState(null)
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

  useEffect(() => {
    loadAccounts()
    loadRecipients()
  }, [user])

  const loadAccounts = async () => {
    try {
      const res = await accountAPI.getByUserId(user?.id || 1)
      const data = res.data.data || []
      setAccounts(data)
      if (data.length > 0) {
        setFromAccount(data[0])
      }
    } catch (error) {
      console.error('Failed to load accounts:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadRecipients = async () => {
    try {
      const res = await recipientAPI.getByUserId(user?.id || 1)
      const data = res.data.data || []
      setRecipients(data)
    } catch (error) {
      console.error('Failed to load recipients:', error)
    }
  }

  const lookupAccount = async (accountNumber) => {
    if (!accountNumber || accountNumber.length < 10) return

    setLookingUpAccount(true)
    setMessage(null)

    // 建立收件人查詢追蹤 span
    const span = startSpan('transfer.lookup_recipient')
    setSpanAttributes(span, {
      'transfer.recipient_account': maskAccount(accountNumber),
      'transfer.source_currency': fromAccount?.currency,
    })

    try {
      const res = await accountAPI.getPublicInfo(accountNumber)
      const accountInfo = res.data.data

      // 檢查幣別是否匹配
      if (accountInfo.currency !== fromAccount?.currency) {
        setSpanAttributes(span, { 'error.reason': 'currency_mismatch' })
        endSpan(span, 'ERROR', 'Currency mismatch')
        setMessage({
          type: 'error',
          text: `Account currency (${accountInfo.currency}) does not match source account (${fromAccount?.currency}). Please use Exchange feature for cross-currency transfers.`
        })
        setToAccount(null)
        return
      }

      // 設定為轉帳目標
      setSpanAttributes(span, {
        'transfer.recipient_currency': accountInfo.currency,
        'transfer.recipient_found': true,
      })
      endSpan(span, 'OK')
      setToAccount({
        accountId: accountInfo.accountId,
        accountNumber: accountInfo.accountNumber,
        currency: accountInfo.currency,
        status: accountInfo.status,
        fullName: accountInfo.fullName,
      })
    } catch (error) {
      setSpanAttributes(span, {
        'error.type': error.name || 'Error',
        'error.message': error.response?.data?.error?.message || error.message,
      })
      endSpan(span, 'ERROR', error.response?.data?.error?.message || 'Account not found')
      setMessage({
        type: 'error',
        text: error.response?.data?.error?.message || 'Account not found'
      })
      setToAccount(null)
    } finally {
      setLookingUpAccount(false)
    }
  }

  const handleContinue = (e) => {
    e.preventDefault()
    if (!fromAccount || !toAccount) return
    setStep(2) // Go to Confirm step
    window.scrollTo(0, 0)
  }

  const handleConfirm = async () => {
    if (!fromAccount || !toAccount) return

    setSubmitting(true)
    setMessage(null)

    // 保存原始帳戶資料，避免被 loadAccounts() 覆蓋
    const originalFromAccount = {...fromAccount}
    const originalToAccount = {...toAccount}
    const originalAmount = amount
    const originalNote = note

    // 建立轉帳提交追蹤 span
    const span = startSpan('transfer.submit')
    setSpanAttributes(span, {
      'transfer.from_account': maskAccount(fromAccount.accountNumber),
      'transfer.to_account': maskAccount(toAccount.accountNumber),
      'transfer.amount': parseFloat(amount.replace(/,/g, '')),
      'transfer.currency': fromAccount.currency,
    })

    try {
      const response = await transferAPI.create({
        fromAccountId: fromAccount.accountId,
        toAccountId: toAccount.accountId,
        amount: parseFloat(amount.replace(/,/g, '')),
        currency: fromAccount.currency,
        description: note || 'Transfer',
      })

      // 記錄成功屬性
      setSpanAttributes(span, {
        'transfer.id': response.data.data?.transferId,
        'transfer.status': response.data.data?.status || 'COMPLETED',
      })
      endSpan(span, 'OK')

      // 將原始資料附加到 transferResult 中
      setTransferResult({
        ...response.data.data,
        fromAccount: originalFromAccount,
        toAccount: originalToAccount,
        amount: originalAmount,
        note: originalNote,
      })
      setStep(3) // Go to Complete step
      loadAccounts() // Refresh balances
      window.scrollTo(0, 0)
    } catch (error) {
      const errorType = getErrorType(error)
      setSpanAttributes(span, {
        'error.type': errorType,
        'error.message': error.response?.data?.error?.message || error.message,
      })
      endSpan(span, 'ERROR', error.response?.data?.error?.message || 'Transfer failed')

      // 設定失敗交易資訊並導向失敗頁面
      setFailedTransaction({
        errorType,
        fromAccount: originalFromAccount,
        toAccount: originalToAccount,
        amount: parseFloat(originalAmount.replace(/,/g, '')),
        note: originalNote,
        timestamp: new Date(),
      })
      setStep(4) // 導向失敗頁面
      window.scrollTo(0, 0)
    } finally {
      setSubmitting(false)
    }
  }

  const handleNewTransfer = () => {
    setStep(1)
    setToAccount(null)
    setToAccountNumber('')
    setAmount('1000')
    setNote('')
    setMessage(null)
    setTransferResult(null)
    setFailedTransaction(null)
    window.scrollTo(0, 0)
  }

  const handleBack = () => {
    if (step > 1) {
      setStep(step - 1)
      setMessage(null)
      window.scrollTo(0, 0)
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

  const quickAmounts = ['100', '500', '1000', '5000']

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
        <h1 className="text-3xl font-heading font-bold text-text mb-2">{t('transfer.title')}</h1>
        <p className="text-text/60">{t('transfer.subtitle')}</p>
      </div>

      <div className={`grid grid-cols-1 ${step === 1 ? 'lg:grid-cols-3' : ''} gap-6`}>
        {/* Transfer Form */}
        <div className={step === 1 ? 'lg:col-span-2' : ''}>
          <form onSubmit={handleContinue} className="bg-white rounded-2xl shadow-sm border border-border p-6">
            {/* Step Indicator */}
            <div className="flex items-center justify-between mb-8">
              <div className="flex items-center">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold ${
                  step >= 1 ? 'bg-primary text-white' : 'bg-border text-text/50'
                }`}>1</div>
                <span className={`ml-2 ${step >= 1 ? 'font-medium text-text' : 'text-text/50'}`}>{t('transfer.details')}</span>
              </div>
              <div className={`flex-1 h-0.5 mx-4 ${step >= 2 ? (step === 4 ? 'bg-red-500' : 'bg-primary') : 'bg-border'}`}></div>
              <div className="flex items-center">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold ${
                  step >= 2 ? (step === 4 ? 'bg-red-500 text-white' : 'bg-primary text-white') : 'bg-border text-text/50'
                }`}>2</div>
                <span className={`ml-2 ${step >= 2 ? 'font-medium text-text' : 'text-text/50'}`}>{t('transfer.confirm')}</span>
              </div>
              <div className={`flex-1 h-0.5 mx-4 ${step >= 3 ? (step === 4 ? 'bg-red-500' : 'bg-primary') : 'bg-border'}`}></div>
              <div className="flex items-center">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold ${
                  step === 3 ? 'bg-primary text-white' : step === 4 ? 'bg-red-500 text-white' : 'bg-border text-text/50'
                }`}>3</div>
                <span className={`ml-2 ${step >= 3 ? 'font-medium text-text' : 'text-text/50'}`}>{t('transfer.complete')}</span>
              </div>
            </div>

            {message && (
              <div className={`mb-6 p-4 rounded-xl ${
                message.type === 'success'
                  ? 'bg-green-50 border border-green-200 text-green-600'
                  : 'bg-red-50 border border-red-200 text-red-600'
              }`}>
                {message.text}
              </div>
            )}

            {/* Step 1: Details */}
            {step === 1 && (
              <>
            {/* From Account */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-text mb-2">{t('transfer.fromAccount')}</label>
              <select
                value={fromAccount?.accountId || ''}
                onChange={(e) => setFromAccount(accounts.find(a => a.accountId === parseInt(e.target.value)))}
                className="w-full px-4 py-4 bg-surface border border-border rounded-xl appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200"
              >
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {account.currency} Account (**** {account.accountNumber?.slice(-4) || '0000'}) - {formatCurrency(account.balance, account.currency)}
                  </option>
                ))}
              </select>
            </div>

            {/* Transfer Arrow */}
            <div className="flex justify-center my-4">
              <div className="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
                <ArrowDownIcon className="w-5 h-5 text-primary" />
              </div>
            </div>

            {/* To Recipient */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-text mb-2">
                {t('transfer.toRecipient')}
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={toAccountNumber}
                  onChange={(e) => setToAccountNumber(e.target.value)}
                  placeholder={t('transfer.enterAccountNumber')}
                  className="flex-1 px-4 py-3 bg-surface border border-border rounded-xl text-text placeholder-text/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200"
                />
                <button
                  type="button"
                  onClick={() => lookupAccount(toAccountNumber)}
                  disabled={lookingUpAccount || !toAccountNumber}
                  className="px-6 py-3 bg-primary/10 border border-primary text-primary rounded-xl font-medium hover:bg-primary/20 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {lookingUpAccount ? t('transfer.lookingUp') : t('transfer.lookup')}
                </button>
              </div>

              {/* Display recipient info after lookup */}
              {toAccount && (
                <div className="mt-3 p-4 bg-primary/5 border border-primary/20 rounded-xl">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <div className={`w-10 h-10 ${getCurrencyIcon(toAccount.currency).bg} rounded-full flex items-center justify-center`}>
                        <span className={`${getCurrencyIcon(toAccount.currency).text} font-semibold`}>
                          {getCurrencyIcon(toAccount.currency).symbol}
                        </span>
                      </div>
                      <div>
                        <p className="font-medium text-text">
                          {toAccount.fullName || toAccount.currency + ' Account'}
                        </p>
                        <p className="text-sm text-text/60">{toAccount.accountNumber}</p>
                        <p className="text-xs text-text/50">{toAccount.currency}</p>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setToAccount(null)
                        setToAccountNumber('')
                      }}
                      className="text-sm text-text/50 hover:text-text"
                    >
                      {t('common.clear')}
                    </button>
                  </div>
                </div>
              )}

              <p className="mt-2 text-sm text-text/50">
                {t('transfer.orSelectRecipient')}
              </p>
            </div>

            {/* Amount */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-text mb-2">{t('transfer.amount')}</label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-2xl font-semibold text-text/50">
                  {getCurrencyIcon(fromAccount?.currency).symbol}
                </span>
                <input
                  type="text"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value.replace(/[^0-9.,]/g, ''))}
                  className="w-full pl-12 pr-24 py-4 bg-surface border border-border rounded-xl text-2xl font-semibold text-text focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200"
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-text/50">
                  {fromAccount?.currency || 'USD'}
                </span>
              </div>
              <p className="mt-2 text-sm text-text/50">
                {t('transfer.available')}: {formatCurrency(fromAccount?.balance || 0, fromAccount?.currency)}
              </p>
            </div>

            {/* Quick Amount Buttons */}
            <div className="flex flex-wrap gap-2 mb-6">
              {quickAmounts.map((qa) => (
                <button
                  key={qa}
                  type="button"
                  onClick={() => setAmount(qa)}
                  className={`px-4 py-2 rounded-lg text-sm transition-colors duration-200 cursor-pointer ${
                    amount === qa
                      ? 'bg-primary/10 border border-primary text-primary font-medium'
                      : 'bg-surface border border-border text-text hover:border-primary hover:bg-primary/5'
                  }`}
                >
                  ${qa}
                </button>
              ))}
              <button
                type="button"
                onClick={() => setAmount(String(fromAccount?.balance || 0))}
                className="px-4 py-2 bg-surface border border-border rounded-lg text-sm text-text hover:border-primary hover:bg-primary/5 transition-colors duration-200 cursor-pointer"
              >
                {t('common.max')}
              </button>
            </div>

            {/* Note */}
            <div className="mb-8">
              <label className="block text-sm font-medium text-text mb-2">{t('transfer.noteOptional')}</label>
              <textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder={t('transfer.addNoteForTransfer')}
                rows="2"
                className="w-full px-4 py-3 bg-surface border border-border rounded-xl text-text placeholder-text/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200 resize-none"
              />
            </div>

            {/* Step 1 Submit Button */}
            <button
              type="submit"
              disabled={!fromAccount || !toAccount}
              className="w-full py-4 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span>{t('transfer.continueToConfirm')}</span>
              <ArrowRightIcon className="w-5 h-5" />
            </button>
            </>
            )}

            {/* Step 2: Confirm */}
            {step === 2 && (
              <>
                <div className="space-y-6">
                  <h3 className="text-xl font-heading font-semibold text-text">Confirm Transfer</h3>

                  {/* Transfer Details */}
                  <div className="bg-surface rounded-xl p-6 space-y-4">
                    <div className="flex justify-between items-center">
                      <span className="text-text/60">From Account</span>
                      <span className="font-medium font-mono text-text">{fromAccount?.accountNumber}</span>
                    </div>
                    <div className="flex justify-center">
                      <ArrowDownIcon className="w-5 h-5 text-primary" />
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-text/60">To Account</span>
                      <span className="font-medium font-mono text-text">{toAccount?.accountNumber}</span>
                    </div>
                    <div className="border-t border-border pt-4"></div>
                    <div className="flex justify-between items-center">
                      <span className="text-text/60">Amount</span>
                      <span className="text-2xl font-bold text-text">
                        {formatCurrency(parseFloat(amount.replace(/,/g, '')) || 0, fromAccount?.currency)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-text/60">Currency</span>
                      <span className="font-medium text-text">{fromAccount?.currency}</span>
                    </div>
                    {note && (
                      <div className="flex justify-between items-start">
                        <span className="text-text/60">Note</span>
                        <span className="text-text text-right max-w-xs">{note}</span>
                      </div>
                    )}
                    <div className="flex justify-between items-center">
                      <span className="text-text/60">Fee</span>
                      <span className="text-green-600 font-medium">Free</span>
                    </div>
                    <div className="border-t border-border pt-4"></div>
                    <div className="flex justify-between items-center">
                      <span className="font-semibold text-text">Total Debit</span>
                      <span className="text-2xl font-bold text-text">
                        {formatCurrency(parseFloat(amount.replace(/,/g, '')) || 0, fromAccount?.currency)}
                      </span>
                    </div>
                  </div>

                  {/* Buttons */}
                  <div className="flex gap-4">
                    <button
                      type="button"
                      onClick={handleBack}
                      className="flex-1 py-4 bg-surface border border-border text-text rounded-xl font-semibold hover:bg-border/50 transition-colors duration-200 cursor-pointer"
                    >
                      Back
                    </button>
                    <button
                      type="button"
                      onClick={handleConfirm}
                      disabled={submitting}
                      className="flex-1 py-4 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {submitting ? (
                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                      ) : (
                        <>
                          <span>Confirm Transfer</span>
                          <ArrowRightIcon className="w-5 h-5" />
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </>
            )}

            {/* Step 3: Complete */}
            {step === 3 && transferResult && (
              <>
                <div className="space-y-6">
                  {/* Success Message */}
                  <div className="bg-green-50 border border-green-200 rounded-xl p-6 text-center">
                    <div className="w-16 h-16 bg-green-500 rounded-full flex items-center justify-center mx-auto mb-4">
                      <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <h3 className="text-2xl font-heading font-bold text-green-700 mb-2">Transfer Successful!</h3>
                    <p className="text-green-600">Your money has been transferred successfully</p>
                  </div>

                  {/* Transfer Details */}
                  <div className="bg-surface rounded-xl p-6 space-y-4">
                    <h4 className="font-semibold text-text">Transfer Details</h4>

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">Transaction ID</span>
                      <span className="font-mono text-sm text-text">#{transferResult.transferId}</span>
                    </div>

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">From Account</span>
                      <span className="font-medium font-mono text-text">{transferResult.fromAccount?.accountNumber}</span>
                    </div>

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">To Account</span>
                      <span className="font-medium font-mono text-text">{transferResult.toAccount?.accountNumber}</span>
                    </div>

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">Amount</span>
                      <span className="text-xl font-bold text-text">
                        {formatCurrency(
                          typeof transferResult.amount === 'string'
                            ? parseFloat(transferResult.amount.replace(/,/g, ''))
                            : transferResult.amount,
                          transferResult.fromAccount?.currency
                        )}
                      </span>
                    </div>

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">Currency</span>
                      <span className="font-medium text-text">{transferResult.fromAccount?.currency}</span>
                    </div>

                    {transferResult.note && (
                      <div className="flex justify-between items-start py-2 border-b border-border">
                        <span className="text-text/60">Note</span>
                        <span className="text-text text-right max-w-xs">{transferResult.note}</span>
                      </div>
                    )}

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">Date & Time</span>
                      <span className="text-text">{new Date(transferResult.createdAt || Date.now()).toLocaleString()}</span>
                    </div>

                    <div className="flex justify-between items-center py-2">
                      <span className="text-text/60">Status</span>
                      <span className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">
                        {transferResult.status || 'COMPLETED'}
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
                      View History
                    </button>
                    <button
                      type="button"
                      onClick={handleNewTransfer}
                      className="flex-1 py-4 bg-primary text-white rounded-xl font-semibold hover:bg-primary/90 transition-colors duration-200 cursor-pointer"
                    >
                      New Transfer
                    </button>
                  </div>
                </div>
              </>
            )}

            {/* Step 4: Failed */}
            {step === 4 && failedTransaction && (
              <>
                <div className="space-y-6">
                  {/* Failed Message */}
                  <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
                    <div className="w-16 h-16 bg-red-500 rounded-full flex items-center justify-center mx-auto mb-4">
                      <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </div>
                    <h3 className="text-2xl font-heading font-bold text-red-700 mb-2">{t('errors.transferFailed')}</h3>
                    <p className="text-red-600">{t('errors.cannotComplete')}</p>
                  </div>

                  {/* Error Reason */}
                  <div className="bg-red-50 border border-red-200 rounded-xl p-4">
                    <h4 className="font-semibold text-red-700 mb-2">{t('errors.failureReason')}</h4>
                    <p className="text-lg font-medium text-red-800">
                      {t(`errors.types.${failedTransaction.errorType}.title`)}
                    </p>
                    <p className="text-red-600 text-sm mt-1">
                      {t(`errors.types.${failedTransaction.errorType}.description`)}
                    </p>
                  </div>

                  {/* Transaction Summary */}
                  <div className="bg-surface rounded-xl p-6 space-y-4">
                    <h4 className="font-semibold text-text">{t('errors.transactionSummary')}</h4>

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">{t('transfer.fromAccount')}</span>
                      <span className="font-medium font-mono text-text">{failedTransaction.fromAccount?.accountNumber}</span>
                    </div>

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">{t('transfer.toAccount')}</span>
                      <span className="font-medium font-mono text-text">{failedTransaction.toAccount?.accountNumber}</span>
                    </div>

                    <div className="flex justify-between items-center py-2 border-b border-border">
                      <span className="text-text/60">{t('transfer.amount')}</span>
                      <span className="text-xl font-bold text-text">
                        {formatCurrency(failedTransaction.amount, failedTransaction.fromAccount?.currency)}
                      </span>
                    </div>

                    {failedTransaction.note && (
                      <div className="flex justify-between items-start py-2 border-b border-border">
                        <span className="text-text/60">{t('transfer.noteOptional')}</span>
                        <span className="text-text text-right max-w-xs">{failedTransaction.note}</span>
                      </div>
                    )}

                    <div className="flex justify-between items-center py-2">
                      <span className="text-text/60">{t('transfer.dateTime')}</span>
                      <span className="text-text">{failedTransaction.timestamp.toLocaleString()}</span>
                    </div>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex gap-4">
                    <button
                      type="button"
                      onClick={handleNewTransfer}
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
              </>
            )}
          </form>
        </div>

        {/* Sidebar - Only show in Step 1 */}
        {step === 1 && (
          <div className="space-y-6">
            {/* Transfer Summary */}
            <div className="bg-white rounded-2xl shadow-sm border border-border p-6">
              <h3 className="font-heading font-semibold text-text mb-4">Transfer Summary</h3>
              <div className="space-y-4">
                <div className="flex justify-between">
                  <span className="text-text/60">Amount</span>
                  <span className="font-semibold text-text">
                    {formatCurrency(parseFloat(amount.replace(/,/g, '')) || 0, fromAccount?.currency)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text/60">From</span>
                  <span className="text-text font-mono text-sm">{fromAccount?.accountNumber || '-'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text/60">To</span>
                  <span className="text-text font-mono text-sm">{toAccount?.accountNumber || '-'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text/60">Fee</span>
                  <span className="text-green-600 font-medium">Free</span>
                </div>
                <div className="border-t border-border pt-4">
                  <div className="flex justify-between">
                    <span className="font-medium text-text">Total Debit</span>
                    <span className="font-bold text-text text-lg">
                      {formatCurrency(parseFloat(amount.replace(/,/g, '')) || 0, fromAccount?.currency)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Saved Recipients */}
            <div className="bg-white rounded-2xl shadow-sm border border-border p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-heading font-semibold text-text">Saved Recipients</h3>
                <button className="text-sm text-primary hover:text-primary/80 cursor-pointer">Manage</button>
              </div>
              {fromAccount && (
                <p className="text-xs text-text/50 mb-3">
                  Showing {fromAccount.currency} recipients only
                </p>
              )}
              <div className="space-y-3">
                {(() => {
                  const filteredRecipients = recipients.filter(
                    r => r.currency === fromAccount?.currency
                  )

                  if (filteredRecipients.length === 0) {
                    return (
                      <p className="text-sm text-text/50 text-center py-4">
                        {fromAccount
                          ? `No ${fromAccount.currency} recipients saved yet`
                          : 'No saved recipients yet'}
                      </p>
                    )
                  }

                  return filteredRecipients.map((recipient) => {
                    const icon = getCurrencyIcon(recipient.currency)
                    return (
                      <div
                        key={recipient.recipientId}
                        className="flex items-center space-x-3 p-3 rounded-xl hover:bg-surface transition-colors duration-200 cursor-pointer"
                        onClick={() => {
                          // 設定帳號並自動查詢
                          setToAccountNumber(recipient.accountNumber)
                          lookupAccount(recipient.accountNumber)
                        }}
                      >
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center ${icon.bg}`}>
                          <span className={`font-semibold ${icon.text}`}>
                            {icon.symbol}
                          </span>
                        </div>
                        <div className="flex-1">
                          <p className="font-medium text-text">{recipient.nickname || `${recipient.currency} Account`}</p>
                          <p className="text-xs text-text/50">**** {recipient.accountNumber?.slice(-4) || '0000'}</p>
                        </div>
                      </div>
                    )
                  })
                })()}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
