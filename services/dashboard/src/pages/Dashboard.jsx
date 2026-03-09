import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { accountAPI, transactionAPI, exchangeAPI, depositWithdrawAPI, analyticsAPI } from '../services/api'
import { startSpan, endSpan, setSpanAttributes } from '../tracing'
import { formatCurrency } from '../utils/formatCurrency'
import {
  ArrowsRightLeftIcon,
  CurrencyDollarIcon,
  PlusIcon,
  MinusIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  XMarkIcon,
  ChartBarIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  CheckCircleIcon
} from '@heroicons/react/24/outline'

export default function Dashboard() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [exchangeRates, setExchangeRates] = useState([])
  const [financialSummary, setFinancialSummary] = useState(null)
  const [loading, setLoading] = useState(true)

  // Modal state for deposit/withdraw
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('deposit') // 'deposit' or 'withdraw'
  const [selectedAccountId, setSelectedAccountId] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')
  const [transactionResult, setTransactionResult] = useState(null) // 儲存成功交易結果
  const [failedTransaction, setFailedTransaction] = useState(null) // 儲存失敗交易資訊

  // 解析錯誤類型
  const getErrorType = (error) => {
    const errorType = error.response?.data?.error?.type
    if (errorType) return errorType
    // 從錯誤訊息中推斷類型
    const message = error.response?.data?.error?.message || error.response?.data?.message || error.message || ''
    if (message.toLowerCase().includes('insufficient')) return 'INSUFFICIENT_BALANCE'
    if (message.toLowerCase().includes('not found')) return 'ACCOUNT_NOT_FOUND'
    return 'UNKNOWN'
  }

  useEffect(() => {
    loadData()
  }, [user])

  const loadData = async () => {
    try {
      const [accountsRes, ratesRes, analyticsRes] = await Promise.all([
        accountAPI.getByUserId(user?.id || 1),
        exchangeAPI.getRates().catch(() => ({ data: { data: [] } })),
        analyticsAPI.getSummary().catch(() => ({ data: { data: null } }))
      ])

      const userAccounts = accountsRes.data.data || []
      setAccounts(userAccounts)
      setExchangeRates(ratesRes.data.data || [])
      setFinancialSummary(analyticsRes.data.data || null)

      // Load recent transactions and filter by user's accounts
      const userAccountIds = userAccounts.map(acc => acc.accountId)
      const txRes = await transactionAPI.getAll({ size: 20, sort: 'createdAt,desc' }).catch(() => ({ data: { data: { content: [] } } }))
      const txData = txRes.data.data
      const allTransactions = txData?.content || txData || []
      // Filter transactions to only show those belonging to user's accounts
      const userTransactions = allTransactions.filter(tx => userAccountIds.includes(tx.accountId))
      setTransactions(userTransactions.slice(0, 10))
    } catch (error) {
      console.error('Failed to load data:', error)
    } finally {
      setLoading(false)
    }
  }

  // 匯率表（外幣換算成台幣）
  const exchangeRatesToTWD = {
    'USD': 31.25,
    'EUR': 33.85,
    'JPY': 0.21,
    'GBP': 39.50,
    'TWD': 1,
  }

  // 分離台幣和外幣帳戶
  const twdAccounts = accounts.filter(acc => acc.currency === 'TWD')
  const foreignAccounts = accounts.filter(acc => acc.currency !== 'TWD')

  // 台幣總額（直接加總）
  const twdTotal = twdAccounts.reduce((sum, acc) => sum + (acc.balance || 0), 0)

  // 外幣總額（換算成台幣）
  const foreignTotalInTWD = foreignAccounts.reduce((sum, acc) => {
    const rate = exchangeRatesToTWD[acc.currency] || 1
    return sum + (acc.balance || 0) * rate
  }, 0)

  // 換算單一帳戶餘額為台幣
  const convertToTWD = (balance, currency) => {
    const rate = exchangeRatesToTWD[currency] || 1
    return balance * rate
  }

  const getCurrencyIcon = (currency) => {
    const icons = {
      USD: { symbol: '$', bg: 'bg-cta/10', text: 'text-cta' },
      EUR: { symbol: '€', bg: 'bg-purple-100', text: 'text-purple-600' },
      TWD: { symbol: 'NT', bg: 'bg-orange-100', text: 'text-orange-600' },
      JPY: { symbol: '¥', bg: 'bg-red-100', text: 'text-red-600' },
      GBP: { symbol: '£', bg: 'bg-blue-100', text: 'text-blue-600' },
    }
    return icons[currency] || icons.USD
  }

  // Modal handlers
  const openModal = (mode) => {
    setModalMode(mode)
    // Convert to string for proper select value matching
    setSelectedAccountId(accounts[0]?.accountId ? String(accounts[0].accountId) : '')
    setAmount('')
    setDescription('')
    setModalError('')
    setTransactionResult(null)
    setFailedTransaction(null)
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setModalError('')
    setTransactionResult(null)
    setFailedTransaction(null)
  }

  const handleModalSubmit = async (e) => {
    e.preventDefault()
    setIsSubmitting(true)
    setModalError('')

    const selectedAccount = accounts.find(acc => acc.accountId === Number(selectedAccountId))
    if (!selectedAccount) {
      setModalError('Please select an account')
      setIsSubmitting(false)
      return
    }

    const numAmount = parseFloat(amount)
    if (isNaN(numAmount) || numAmount <= 0) {
      setModalError('Please enter a valid amount')
      setIsSubmitting(false)
      return
    }

    // 建立追蹤 span
    const spanName = modalMode === 'deposit' ? 'deposit.flow' : 'withdrawal.flow'
    const span = startSpan(spanName)
    setSpanAttributes(span, {
      [`${modalMode}.account_id`]: Number(selectedAccountId),
      [`${modalMode}.amount`]: numAmount,
      [`${modalMode}.currency`]: selectedAccount.currency,
    })

    try {
      const data = {
        accountId: Number(selectedAccountId),
        amount: numAmount,
        currency: selectedAccount.currency,
        description: description || (modalMode === 'deposit' ? 'Deposit' : 'Withdrawal')
      }

      let response
      if (modalMode === 'deposit') {
        response = await depositWithdrawAPI.deposit(data)
      } else {
        response = await depositWithdrawAPI.withdraw(data)
      }

      // 取得新餘額（從 response 或重新載入帳戶）
      const updatedAccounts = await accountAPI.getByUserId(user?.id || 1)
      const updatedAccount = updatedAccounts.data.data?.find(acc => acc.accountId === Number(selectedAccountId))

      // 儲存交易結果
      setTransactionResult({
        amount: numAmount,
        currency: selectedAccount.currency,
        accountName: `${selectedAccount.currency} ${t('dashboard.account')}`,
        newBalance: updatedAccount?.balance || selectedAccount.balance,
        timestamp: new Date()
      })

      // 更新帳戶資料
      setAccounts(updatedAccounts.data.data || [])
      loadData()

      // 追蹤成功
      endSpan(span, 'OK')
    } catch (error) {
      // 追蹤失敗
      const errorType = getErrorType(error)
      endSpan(span, 'ERROR', error.response?.data?.message || error.message)

      // 設定失敗交易資訊
      setFailedTransaction({
        errorType,
        amount: numAmount,
        currency: selectedAccount.currency,
        accountName: `${selectedAccount.currency} ${t('dashboard.account')}`,
        timestamp: new Date()
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    )
  }

  return (
    <div>
      {/* Welcome Section */}
      <div className="mb-8">
        <h1 className="text-3xl font-heading font-bold text-text mb-2">
          {t('dashboard.welcomeBack', { name: user?.username || 'User' })}
        </h1>
        <p className="text-text/60">{t('dashboard.welcomeSubtitle')}</p>
      </div>

      {/* Stats Cards - 台幣與外幣分開顯示 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        {/* 台幣資產卡片 */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-border hover:shadow-md transition-shadow duration-200">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-orange-100 rounded-xl flex items-center justify-center">
              <span className="text-orange-600 font-bold text-lg">NT</span>
            </div>
            <span className="text-xs text-text/50 bg-surface px-2 py-1 rounded-full">{t('dashboard.twdAssets')}</span>
          </div>
          <p className="text-text/60 text-sm mb-1">{t('dashboard.twdTotal')}</p>
          <p className="text-3xl font-heading font-bold text-text">
            {formatCurrency(twdTotal, 'TWD')}
          </p>
          {twdAccounts.length === 0 && (
            <p className="text-text/40 text-sm mt-2">{t('dashboard.noTwdAccount')}</p>
          )}
        </div>

        {/* 外幣資產卡片 */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-border hover:shadow-md transition-shadow duration-200">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-primary/10 rounded-xl flex items-center justify-center">
              <CurrencyDollarIcon className="w-6 h-6 text-primary" />
            </div>
            <span className="text-xs text-text/50 bg-surface px-2 py-1 rounded-full">{t('dashboard.foreignAssets')}</span>
          </div>
          <p className="text-text/60 text-sm mb-1">{t('dashboard.foreignTotal')}</p>
          <p className="text-3xl font-heading font-bold text-text">
            ≈ {formatCurrency(foreignTotalInTWD, 'TWD')}
          </p>
          <p className="text-text/40 text-xs mt-1">{t('dashboard.basedOnRate')}</p>
        </div>
      </div>

      {/* 外幣帳戶明細 */}
      {foreignAccounts.length > 0 && (
        <div className="mb-8">
          <h2 className="text-lg font-heading font-semibold text-text mb-4">{t('dashboard.foreignAccountDetails')}</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {foreignAccounts.map((account) => {
              const icon = getCurrencyIcon(account.currency)
              const twdValue = convertToTWD(account.balance, account.currency)
              return (
                <div
                  key={account.accountId}
                  className="bg-white rounded-2xl p-5 shadow-sm border border-border hover:shadow-md transition-shadow duration-200"
                >
                  <div className="flex items-center justify-between mb-3">
                    <div className={`w-10 h-10 ${icon.bg} rounded-xl flex items-center justify-center`}>
                      <span className={`${icon.text} font-bold text-sm`}>{icon.symbol}</span>
                    </div>
                    <span className="text-xs font-medium text-text/60 bg-surface px-2 py-1 rounded-full">{account.currency}</span>
                  </div>
                  <p className="text-xl font-heading font-bold text-text">
                    {formatCurrency(account.balance, account.currency)}
                  </p>
                  <p className="text-text/50 text-sm mt-1">
                    ≈ {formatCurrency(twdValue, 'TWD')}
                  </p>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Transactions Section */}
        <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm border border-border">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-heading font-semibold text-text">{t('dashboard.recentTransactions')}</h2>
            <Link to="/history" className="text-sm text-primary hover:text-primary/80 cursor-pointer">
              {t('common.viewAll')}
            </Link>
          </div>
          {/* Transactions List */}
          <div className="space-y-3">
            {transactions.length > 0 ? (
              transactions
                .filter(tx => !tx.transactionType.startsWith('EXCHANGE'))
                .slice(0, 5)
                .map((tx) => {
                const isIncome = tx.transactionType === 'CREDIT' || tx.transactionType === 'DEPOSIT' || tx.transactionType === 'TRANSFER_IN'
                const typeConfig = {
                  CREDIT: { icon: ArrowDownIcon, label: t('history.types.deposit'), color: 'text-green-600', bg: 'bg-green-100' },
                  DEBIT: { icon: ArrowUpIcon, label: t('history.types.withdrawal'), color: 'text-red-500', bg: 'bg-red-100' },
                  DEPOSIT: { icon: ArrowDownIcon, label: t('history.types.deposit'), color: 'text-green-600', bg: 'bg-green-100' },
                  WITHDRAWAL: { icon: ArrowUpIcon, label: t('history.types.withdrawal'), color: 'text-red-500', bg: 'bg-red-100' },
                  TRANSFER_IN: { icon: ArrowDownIcon, label: t('history.types.transferIn'), color: 'text-green-600', bg: 'bg-green-100' },
                  TRANSFER_OUT: { icon: ArrowUpIcon, label: t('history.types.transferOut'), color: 'text-red-500', bg: 'bg-red-100' },
                  EXCHANGE_IN: { icon: ArrowsRightLeftIcon, label: t('history.types.exchange'), color: 'text-blue-600', bg: 'bg-blue-100' },
                  EXCHANGE_OUT: { icon: ArrowsRightLeftIcon, label: t('history.types.exchange'), color: 'text-blue-600', bg: 'bg-blue-100' },
                }
                const config = typeConfig[tx.transactionType] || typeConfig.DEBIT
                const IconComponent = config.icon
                return (
                  <div key={tx.transactionId} className="flex items-center justify-between p-3 rounded-xl hover:bg-surface transition-colors">
                    <div className="flex items-center space-x-3">
                      <div className={`w-10 h-10 ${config.bg} rounded-full flex items-center justify-center`}>
                        <IconComponent className={`w-5 h-5 ${config.color}`} />
                      </div>
                      <div>
                        <div className="flex items-center space-x-2">
                          <p className="font-medium text-text">{config.label}</p>
                          <span className="text-xs px-1.5 py-0.5 rounded bg-text/10 text-text/60 font-medium">{tx.currency}</span>
                        </div>
                        <p className="text-xs text-text/50">
                          {new Date(tx.createdAt).toLocaleDateString('zh-TW', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                        </p>
                      </div>
                    </div>
                    <p className={`font-heading font-semibold ${isIncome ? 'text-green-600' : 'text-red-500'}`}>
                      {isIncome ? '+' : '-'}{formatCurrency(Math.abs(tx.amount), tx.currency)}
                    </p>
                  </div>
                )
              })
            ) : (
              <div className="text-center py-8">
                <ArrowsRightLeftIcon className="w-12 h-12 text-text/20 mx-auto mb-3" />
                <p className="text-text/50">{t('dashboard.noTransactions')}</p>
              </div>
            )}
          </div>
        </div>

        {/* Quick Actions */}
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-border">
          <h2 className="text-lg font-heading font-semibold text-text mb-6">{t('dashboard.quickActions')}</h2>
          <div className="space-y-3">
            <Link
              to="/transfer"
              className="w-full flex items-center space-x-4 p-4 rounded-xl bg-primary text-white hover:bg-primary/90 transition-colors duration-200 cursor-pointer"
            >
              <ArrowsRightLeftIcon className="w-6 h-6" />
              <span className="font-medium">{t('dashboard.transferMoney')}</span>
            </Link>
            <Link
              to="/exchange"
              className="w-full flex items-center space-x-4 p-4 rounded-xl border-2 border-primary/20 text-primary hover:bg-primary/5 transition-colors duration-200 cursor-pointer"
            >
              <CurrencyDollarIcon className="w-6 h-6" />
              <span className="font-medium">{t('dashboard.exchangeCurrency')}</span>
            </Link>
            <button
              onClick={() => openModal('deposit')}
              className="w-full flex items-center space-x-4 p-4 rounded-xl border-2 border-primary/20 text-primary hover:bg-primary/5 transition-colors duration-200 cursor-pointer"
            >
              <PlusIcon className="w-6 h-6" />
              <span className="font-medium">{t('dashboard.deposit')}</span>
            </button>
            <button
              onClick={() => openModal('withdraw')}
              className="w-full flex items-center space-x-4 p-4 rounded-xl border-2 border-primary/20 text-primary hover:bg-primary/5 transition-colors duration-200 cursor-pointer"
            >
              <MinusIcon className="w-6 h-6" />
              <span className="font-medium">{t('dashboard.withdraw')}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Financial Health Indicators */}
      <div className="mt-6 bg-white rounded-2xl p-6 shadow-sm border border-border">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-2">
            <ChartBarIcon className="w-5 h-5 text-primary" />
            <h2 className="text-lg font-heading font-semibold text-text">{t('dashboard.financialHealth')}</h2>
          </div>
          <Link to="/history" className="text-sm text-primary hover:text-primary/80 cursor-pointer">
            {t('common.viewAll')}
          </Link>
        </div>

        {financialSummary ? (
          <div className="space-y-6">
            {/* Income & Expense Summary */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Monthly Income */}
              <div className="p-4 rounded-xl bg-green-50 border border-green-100">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-green-700">{t('dashboard.monthlyIncome')}</span>
                  <ArrowUpIcon className="w-4 h-4 text-green-600" />
                </div>
                <p className="text-2xl font-heading font-bold text-green-700">
                  {formatCurrency(financialSummary.currentMonth?.totalIncomeInTwd || 0, 'TWD')}
                </p>
                {financialSummary.comparison?.incomeChangePercent !== undefined && (
                  <div className={`flex items-center mt-1 text-xs ${financialSummary.comparison.incomeChangePercent >= 0 ? 'text-green-600' : 'text-red-500'}`}>
                    {financialSummary.comparison.incomeChangePercent >= 0 ? (
                      <ArrowTrendingUpIcon className="w-3 h-3 mr-1" />
                    ) : (
                      <ArrowTrendingDownIcon className="w-3 h-3 mr-1" />
                    )}
                    <span>{financialSummary.comparison.incomeChangePercent >= 0 ? '+' : ''}{financialSummary.comparison.incomeChangePercent}% {t('dashboard.vsLastMonth')}</span>
                  </div>
                )}
              </div>

              {/* Monthly Expense */}
              <div className="p-4 rounded-xl bg-red-50 border border-red-100">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-red-700">{t('dashboard.monthlyExpense')}</span>
                  <ArrowDownIcon className="w-4 h-4 text-red-600" />
                </div>
                <p className="text-2xl font-heading font-bold text-red-700">
                  {formatCurrency(financialSummary.currentMonth?.totalExpenseInTwd || 0, 'TWD')}
                </p>
                {financialSummary.comparison?.expenseChangePercent !== undefined && (
                  <div className={`flex items-center mt-1 text-xs ${financialSummary.comparison.expenseChangePercent <= 0 ? 'text-green-600' : 'text-red-500'}`}>
                    {financialSummary.comparison.expenseChangePercent <= 0 ? (
                      <ArrowTrendingDownIcon className="w-3 h-3 mr-1" />
                    ) : (
                      <ArrowTrendingUpIcon className="w-3 h-3 mr-1" />
                    )}
                    <span>{financialSummary.comparison.expenseChangePercent >= 0 ? '+' : ''}{financialSummary.comparison.expenseChangePercent}% {t('dashboard.vsLastMonth')}</span>
                  </div>
                )}
              </div>

              {/* Net Change */}
              <div className="p-4 rounded-xl bg-primary/5 border border-primary/10">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-primary">{t('dashboard.netChange')}</span>
                  <ArrowsRightLeftIcon className="w-4 h-4 text-primary" />
                </div>
                <p className={`text-2xl font-heading font-bold ${(financialSummary.currentMonth?.netChangeInTwd || 0) >= 0 ? 'text-green-600' : 'text-red-500'}`}>
                  {(financialSummary.currentMonth?.netChangeInTwd || 0) >= 0 ? '+' : ''}
                  {formatCurrency(financialSummary.currentMonth?.netChangeInTwd || 0, 'TWD')}
                </p>
              </div>
            </div>

            {/* Savings Rate */}
            <div className="p-4 rounded-xl bg-surface">
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm font-medium text-text">{t('dashboard.savingsRate')}</span>
                <div className="flex items-center space-x-2">
                  <span className="text-2xl font-heading font-bold text-primary">
                    {financialSummary.currentMonth?.overallSavingsRate?.toFixed(1) || 0}%
                  </span>
                  {financialSummary.comparison?.savingsRateChange !== undefined && (
                    <span className={`text-xs px-2 py-0.5 rounded-full ${financialSummary.comparison.savingsRateChange >= 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                      {financialSummary.comparison.savingsRateChange >= 0 ? '+' : ''}{financialSummary.comparison.savingsRateChange}%
                    </span>
                  )}
                </div>
              </div>
              {/* Progress Bar */}
              <div className="w-full bg-gray-200 rounded-full h-3">
                <div
                  className="bg-primary h-3 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(Math.max(financialSummary.currentMonth?.overallSavingsRate || 0, 0), 100)}%` }}
                ></div>
              </div>
              <p className="text-xs text-text/50 mt-2">{t('dashboard.savingsRateDesc')}</p>
            </div>
          </div>
        ) : (
          <div className="text-center py-8">
            <ChartBarIcon className="w-12 h-12 text-text/20 mx-auto mb-3" />
            <p className="text-text/50">{t('dashboard.noFinancialData')}</p>
            <p className="text-xs text-text/40 mt-1">{t('dashboard.startTransacting')}</p>
          </div>
        )}
      </div>

      {/* Exchange Rates Widget */}
      <div className="mt-6 bg-white rounded-2xl p-6 shadow-sm border border-border">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-heading font-semibold text-text">{t('dashboard.liveExchangeRates')}</h2>
          <span className="text-xs text-text/40">{t('dashboard.updated')}: {t('dashboard.justNow')}</span>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { pair: 'USD/EUR', rate: '0.9234', change: '+0.12%', positive: true },
            { pair: 'USD/TWD', rate: '31.25', change: '-0.08%', positive: false },
            { pair: 'EUR/TWD', rate: '33.85', change: '+0.05%', positive: true },
            { pair: 'USD/JPY', rate: '148.50', change: '+0.23%', positive: true },
          ].map((item) => (
            <div key={item.pair} className="p-4 rounded-xl bg-surface">
              <p className="text-sm text-text/60 mb-1">{item.pair}</p>
              <p className="text-xl font-heading font-bold text-text">{item.rate}</p>
              <span className={`text-xs ${item.positive ? 'text-green-600' : 'text-red-500'}`}>
                {item.change}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Deposit/Withdraw Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl p-6 w-full max-w-md mx-4 shadow-xl">
            {failedTransaction ? (
              /* 失敗畫面 */
              <>
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-xl font-heading font-semibold text-red-700">
                    {modalMode === 'deposit' ? t('errors.depositFailed') : t('errors.withdrawFailed')}
                  </h2>
                  <button
                    onClick={closeModal}
                    className="p-2 hover:bg-surface rounded-lg transition-colors"
                  >
                    <XMarkIcon className="w-5 h-5 text-text/60" />
                  </button>
                </div>

                <div className="text-center py-4">
                  <div className="w-16 h-16 mx-auto rounded-full flex items-center justify-center bg-red-100">
                    <svg className="w-10 h-10 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </div>
                  <p className="mt-3 text-lg font-medium text-red-700">{t('errors.transactionFailed')}</p>
                </div>

                {/* Error Reason */}
                <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-4">
                  <h4 className="font-semibold text-red-700 mb-1">{t('errors.failureReason')}</h4>
                  <p className="text-red-800 font-medium">
                    {t(`errors.types.${failedTransaction.errorType}.title`)}
                  </p>
                  <p className="text-red-600 text-sm mt-1">
                    {t(`errors.types.${failedTransaction.errorType}.description`)}
                  </p>
                </div>

                <div className="bg-surface rounded-xl p-4 space-y-3 mb-4">
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{modalMode === 'deposit' ? t('dashboard.depositAmount') : t('dashboard.withdrawAmount')}</span>
                    <span className="font-heading font-semibold text-text">
                      {formatCurrency(failedTransaction.amount, failedTransaction.currency)}
                    </span>
                  </div>
                  <div className="border-t border-border"></div>
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{t('dashboard.account')}</span>
                    <span className="font-medium text-text">{failedTransaction.accountName}</span>
                  </div>
                  <div className="border-t border-border"></div>
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{t('dashboard.time')}</span>
                    <span className="text-text">
                      {failedTransaction.timestamp.toLocaleDateString('zh-TW', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                </div>

                <div className="flex gap-3">
                  <button
                    onClick={() => {
                      setFailedTransaction(null)
                      setAmount('')
                    }}
                    className="flex-1 py-3 rounded-xl font-medium transition-colors border border-red-300 text-red-600 hover:bg-red-50"
                  >
                    {t('errors.tryAgain')}
                  </button>
                  <button
                    onClick={() => window.location.href = '/accounts'}
                    className="flex-1 py-3 rounded-xl font-medium transition-colors bg-primary text-white hover:bg-primary/90"
                  >
                    {t('errors.viewMyAccounts')}
                  </button>
                </div>
              </>
            ) : transactionResult ? (
              /* 成功畫面 */
              <>
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-xl font-heading font-semibold text-text">
                    {modalMode === 'deposit' ? t('dashboard.depositSuccess') : t('dashboard.withdrawSuccess')}
                  </h2>
                  <button
                    onClick={closeModal}
                    className="p-2 hover:bg-surface rounded-lg transition-colors"
                  >
                    <XMarkIcon className="w-5 h-5 text-text/60" />
                  </button>
                </div>

                <div className="text-center py-4">
                  <div className={`w-16 h-16 mx-auto rounded-full flex items-center justify-center ${modalMode === 'deposit' ? 'bg-green-100' : 'bg-red-100'}`}>
                    <CheckCircleIcon className={`w-10 h-10 ${modalMode === 'deposit' ? 'text-green-600' : 'text-red-500'}`} />
                  </div>
                  <p className="mt-3 text-lg font-medium text-text">{t('dashboard.transactionComplete')}</p>
                </div>

                <div className="bg-surface rounded-xl p-4 space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{modalMode === 'deposit' ? t('dashboard.depositAmount') : t('dashboard.withdrawAmount')}</span>
                    <span className={`font-heading font-semibold ${modalMode === 'deposit' ? 'text-green-600' : 'text-red-500'}`}>
                      {modalMode === 'deposit' ? '+' : '-'}{formatCurrency(transactionResult.amount, transactionResult.currency)}
                    </span>
                  </div>
                  <div className="border-t border-border"></div>
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{t('dashboard.account')}</span>
                    <span className="font-medium text-text">{transactionResult.accountName}</span>
                  </div>
                  <div className="border-t border-border"></div>
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{t('dashboard.newBalance')}</span>
                    <span className="font-heading font-semibold text-text">
                      {formatCurrency(transactionResult.newBalance, transactionResult.currency)}
                    </span>
                  </div>
                  <div className="border-t border-border"></div>
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{t('dashboard.time')}</span>
                    <span className="text-text">
                      {transactionResult.timestamp.toLocaleDateString('zh-TW', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                </div>

                <button
                  onClick={closeModal}
                  className={`w-full mt-6 py-3 rounded-xl font-medium transition-colors ${modalMode === 'deposit'
                    ? 'bg-green-600 hover:bg-green-700 text-white'
                    : 'bg-red-500 hover:bg-red-600 text-white'
                  }`}
                >
                  {t('common.done')}
                </button>
              </>
            ) : (
              /* 輸入表單 */
              <>
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-xl font-heading font-semibold text-text">
                    {modalMode === 'deposit' ? t('dashboard.depositFunds') : t('dashboard.withdrawFunds')}
                  </h2>
                  <button
                    onClick={closeModal}
                    className="p-2 hover:bg-surface rounded-lg transition-colors"
                  >
                    <XMarkIcon className="w-5 h-5 text-text/60" />
                  </button>
                </div>

                <form onSubmit={handleModalSubmit} className="space-y-4">
                  {/* Account Selection */}
                  <div>
                    <label className="block text-sm font-medium text-text/70 mb-2">
                      {t('dashboard.selectAccount')}
                    </label>
                    <select
                      value={selectedAccountId}
                      onChange={(e) => setSelectedAccountId(e.target.value)}
                      className="w-full px-4 py-3 rounded-xl border border-border focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
                    >
                      {accounts.length === 0 ? (
                        <option value="">{t('dashboard.noAccountsAvailable')}</option>
                      ) : (
                        accounts.map((acc) => (
                          <option key={acc.accountId} value={String(acc.accountId)}>
                            {acc.currency} Account - {formatCurrency(acc.balance, acc.currency)}
                          </option>
                        ))
                      )}
                    </select>
                  </div>

                  {/* Amount Input */}
                  <div>
                    <label className="block text-sm font-medium text-text/70 mb-2">
                      {t('dashboard.amount')}
                    </label>
                    <input
                      type="number"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder={t('dashboard.enterAmount')}
                      min="0.01"
                      step="0.01"
                      className="w-full px-4 py-3 rounded-xl border border-border focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
                    />
                  </div>

                  {/* Quick Amount Buttons */}
                  <div className="flex gap-2">
                    {[100, 500, 1000, 5000].map((quickAmount) => (
                      <button
                        key={quickAmount}
                        type="button"
                        onClick={() => setAmount(quickAmount.toString())}
                        className="flex-1 py-2 text-sm border border-border rounded-lg hover:bg-surface transition-colors"
                      >
                        ${quickAmount}
                      </button>
                    ))}
                  </div>

                  {/* Description Input */}
                  <div>
                    <label className="block text-sm font-medium text-text/70 mb-2">
                      {t('dashboard.descriptionOptional')}
                    </label>
                    <input
                      type="text"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder={t('dashboard.addNote')}
                      className="w-full px-4 py-3 rounded-xl border border-border focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
                    />
                  </div>

                  {/* Error Message */}
                  {modalError && (
                    <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
                      {modalError}
                    </div>
                  )}

                  {/* Submit Button */}
                  <button
                    type="submit"
                    disabled={isSubmitting || accounts.length === 0}
                    className={`w-full py-3 rounded-xl font-medium transition-colors ${modalMode === 'deposit'
                      ? 'bg-green-600 hover:bg-green-700 text-white'
                      : 'bg-red-500 hover:bg-red-600 text-white'
                      } disabled:opacity-50 disabled:cursor-not-allowed`}
                  >
                    {isSubmitting ? t('dashboard.processing') : modalMode === 'deposit' ? t('dashboard.deposit') : t('dashboard.withdraw')}
                  </button>
                </form>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
