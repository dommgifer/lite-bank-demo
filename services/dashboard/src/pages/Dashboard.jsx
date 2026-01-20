import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { accountAPI, transactionAPI, exchangeAPI, depositWithdrawAPI } from '../services/api'
import {
  ArrowsRightLeftIcon,
  CurrencyDollarIcon,
  PlusIcon,
  MinusIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  XMarkIcon
} from '@heroicons/react/24/outline'

export default function Dashboard() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [exchangeRates, setExchangeRates] = useState([])
  const [loading, setLoading] = useState(true)

  // Modal state for deposit/withdraw
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('deposit') // 'deposit' or 'withdraw'
  const [selectedAccountId, setSelectedAccountId] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')
  const [modalSuccess, setModalSuccess] = useState('')

  useEffect(() => {
    loadData()
  }, [user])

  const loadData = async () => {
    try {
      const [accountsRes, ratesRes] = await Promise.all([
        accountAPI.getByUserId(user?.id || 1),
        exchangeAPI.getRates().catch(() => ({ data: { data: [] } }))
      ])

      setAccounts(accountsRes.data.data || [])
      setExchangeRates(ratesRes.data.data || [])

      // Load transactions for first account (API returns paginated response)
      if (accountsRes.data.data?.length > 0) {
        const txRes = await transactionAPI.getByAccountId(accountsRes.data.data[0].id)
        // Handle paginated response: data.content contains the transactions array
        const txData = txRes.data.data
        setTransactions(txData?.content || txData || [])
      }
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

  const formatCurrency = (amount, currency = 'USD') => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
    }).format(amount)
  }

  // Modal handlers
  const openModal = (mode) => {
    setModalMode(mode)
    // Convert to string for proper select value matching
    setSelectedAccountId(accounts[0]?.id ? String(accounts[0].id) : '')
    setAmount('')
    setDescription('')
    setModalError('')
    setModalSuccess('')
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setModalError('')
    setModalSuccess('')
  }

  const handleModalSubmit = async (e) => {
    e.preventDefault()
    setIsSubmitting(true)
    setModalError('')
    setModalSuccess('')

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

    try {
      const data = {
        accountId: Number(selectedAccountId),
        amount: numAmount,
        currency: selectedAccount.currency,
        description: description || (modalMode === 'deposit' ? 'Deposit' : 'Withdrawal')
      }

      if (modalMode === 'deposit') {
        await depositWithdrawAPI.deposit(data)
        setModalSuccess(`Successfully deposited ${formatCurrency(numAmount, selectedAccount.currency)}`)
      } else {
        await depositWithdrawAPI.withdraw(data)
        setModalSuccess(`Successfully withdrew ${formatCurrency(numAmount, selectedAccount.currency)}`)
      }

      // Reload data after successful transaction
      setTimeout(() => {
        loadData()
        closeModal()
      }, 1500)
    } catch (error) {
      setModalError(error.response?.data?.message || `Failed to ${modalMode}. Please try again.`)
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
        {/* Chart Section */}
        <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm border border-border">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-heading font-semibold text-text">{t('dashboard.balanceTrend')}</h2>
            <div className="flex space-x-2">
              <button className="px-3 py-1 text-sm bg-primary/10 text-primary rounded-lg cursor-pointer">{t('dashboard.week')}</button>
              <button className="px-3 py-1 text-sm text-text/60 hover:bg-primary/5 rounded-lg cursor-pointer">{t('dashboard.month')}</button>
              <button className="px-3 py-1 text-sm text-text/60 hover:bg-primary/5 rounded-lg cursor-pointer">{t('dashboard.year')}</button>
            </div>
          </div>
          {/* Chart Placeholder */}
          <div className="h-64 bg-gradient-to-b from-primary/5 to-transparent rounded-xl flex items-end justify-around px-4 pb-4">
            <div className="w-8 bg-primary/20 rounded-t-lg" style={{ height: '40%' }}></div>
            <div className="w-8 bg-primary/30 rounded-t-lg" style={{ height: '55%' }}></div>
            <div className="w-8 bg-primary/40 rounded-t-lg" style={{ height: '45%' }}></div>
            <div className="w-8 bg-primary/50 rounded-t-lg" style={{ height: '70%' }}></div>
            <div className="w-8 bg-primary/60 rounded-t-lg" style={{ height: '60%' }}></div>
            <div className="w-8 bg-primary/70 rounded-t-lg" style={{ height: '85%' }}></div>
            <div className="w-8 bg-primary rounded-t-lg" style={{ height: '75%' }}></div>
          </div>
          <div className="flex justify-around mt-2 text-xs text-text/40">
            <span>Mon</span><span>Tue</span><span>Wed</span><span>Thu</span><span>Fri</span><span>Sat</span><span>Sun</span>
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

      {/* Recent Transactions */}
      <div className="mt-6 bg-white rounded-2xl p-6 shadow-sm border border-border">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-heading font-semibold text-text">{t('dashboard.recentTransactions')}</h2>
          <Link to="/history" className="text-sm text-primary hover:text-primary/80 cursor-pointer">
            {t('common.viewAll')}
          </Link>
        </div>
        <div className="space-y-4">
          {transactions.length === 0 ? (
            <p className="text-center text-text/50 py-8">{t('dashboard.noTransactions')}</p>
          ) : (
            transactions.slice(0, 4).map((tx) => (
              <div
                key={tx.id}
                className="flex items-center justify-between p-4 rounded-xl hover:bg-surface transition-colors duration-200 cursor-pointer"
              >
                <div className="flex items-center space-x-4">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${tx.transactionType === 'WITHDRAWAL'
                    ? 'bg-red-100'
                    : tx.transactionType === 'DEPOSIT'
                      ? 'bg-green-100'
                      : tx.transactionType === 'EXCHANGE'
                        ? 'bg-purple-100'
                        : tx.amount > 0
                          ? 'bg-green-100'
                          : 'bg-red-100'
                    }`}>
                    {tx.transactionType === 'WITHDRAWAL' ? (
                      <ArrowDownIcon className="w-6 h-6 text-red-500" />
                    ) : tx.transactionType === 'DEPOSIT' ? (
                      <ArrowUpIcon className="w-6 h-6 text-green-600" />
                    ) : tx.transactionType === 'EXCHANGE' ? (
                      <ArrowsRightLeftIcon className="w-6 h-6 text-purple-600" />
                    ) : tx.amount > 0 ? (
                      <ArrowUpIcon className="w-6 h-6 text-green-600" />
                    ) : (
                      <ArrowDownIcon className="w-6 h-6 text-red-500" />
                    )}
                  </div>
                  <div>
                    <p className="font-medium text-text">{tx.description || tx.transactionType || tx.type}</p>
                    <p className="text-sm text-text/50">
                      {new Date(tx.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
                <span className={`font-semibold ${tx.transactionType === 'WITHDRAWAL'
                  ? 'text-red-500'
                  : tx.transactionType === 'DEPOSIT'
                    ? 'text-green-600'
                    : tx.amount > 0
                      ? 'text-green-600'
                      : 'text-red-500'
                  }`}>
                  {tx.transactionType === 'WITHDRAWAL' ? '-' : tx.transactionType === 'DEPOSIT' ? '+' : tx.amount > 0 ? '+' : ''}{formatCurrency(Math.abs(tx.amount), tx.currency)}
                </span>
              </div>
            ))
          )}
        </div>
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

              {/* Error/Success Messages */}
              {modalError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
                  {modalError}
                </div>
              )}
              {modalSuccess && (
                <div className="p-3 bg-green-50 border border-green-200 rounded-xl text-green-600 text-sm">
                  {modalSuccess}
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
          </div>
        </div>
      )}
    </div>
  )
}
