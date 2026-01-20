import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { accountAPI, transactionAPI } from '../services/api'
import {
  MagnifyingGlassIcon,
  ArrowDownTrayIcon,
  PlusIcon,
  MinusIcon,
  ArrowsRightLeftIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  XMarkIcon
} from '@heroicons/react/24/outline'

export default function History() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [filteredTransactions, setFilteredTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [typeFilter, setTypeFilter] = useState('All Types')
  const [accountFilter, setAccountFilter] = useState('All Accounts')
  const [dateFilter, setDateFilter] = useState('Last 30 days')
  const [currentPage, setCurrentPage] = useState(1)
  const itemsPerPage = 10

  useEffect(() => {
    loadData()
  }, [user])

  useEffect(() => {
    filterTransactions()
  }, [transactions, searchQuery, typeFilter, accountFilter])

  const loadData = async () => {
    try {
      const accountsRes = await accountAPI.getByUserId(user?.id || 1)
      const accountsData = accountsRes.data.data || []
      setAccounts(accountsData)

      // Load transactions for all accounts (API returns paginated response)
      const allTransactions = []
      for (const account of accountsData) {
        try {
          const txRes = await transactionAPI.getByAccountId(account.accountId)
          // Handle paginated response: data.content contains the transactions array
          const txData = txRes.data.data
          const txs = txData?.content || txData || []
          allTransactions.push(...txs.map(tx => ({ ...tx, accountCurrency: account.currency })))
        } catch (e) {
          // Skip failed requests
        }
      }

      // Sort by date descending
      allTransactions.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      setTransactions(allTransactions)
      setFilteredTransactions(allTransactions)
    } catch (error) {
      console.error('Failed to load data:', error)
    } finally {
      setLoading(false)
    }
  }

  const filterTransactions = () => {
    let filtered = [...transactions]

    // Search filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase()
      filtered = filtered.filter(tx =>
        tx.description?.toLowerCase().includes(query) ||
        (tx.transactionType || tx.type)?.toLowerCase().includes(query)
      )
    }

    // Type filter
    if (typeFilter !== 'All Types') {
      filtered = filtered.filter(tx => (tx.transactionType || tx.type) === typeFilter.toUpperCase())
    }

    // Account filter
    if (accountFilter !== 'All Accounts') {
      filtered = filtered.filter(tx => tx.accountCurrency === accountFilter.replace(' Account', ''))
    }

    setFilteredTransactions(filtered)
    setCurrentPage(1)
  }

  const formatCurrency = (amount, currency = 'USD') => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
    }).format(amount)
  }

  const getTypeIcon = (type, amount) => {
    if (type === 'WITHDRAWAL' || type === 'TRANSFER_OUT' || type === 'EXCHANGE_OUT') {
      return { icon: MinusIcon, bg: 'bg-red-100', color: 'text-red-500' }
    }
    if (type === 'DEPOSIT' || type === 'TRANSFER_IN' || type === 'EXCHANGE_IN') {
      return { icon: PlusIcon, bg: 'bg-green-100', color: 'text-green-600' }
    }
    if (type === 'EXCHANGE') {
      return { icon: ArrowsRightLeftIcon, bg: 'bg-purple-100', color: 'text-purple-600' }
    }
    if (type === 'TRANSFER') {
      return { icon: amount > 0 ? ArrowUpIcon : ArrowDownIcon, bg: 'bg-blue-100', color: 'text-blue-600' }
    }
    // Fallback based on amount
    if (amount > 0) {
      return { icon: PlusIcon, bg: 'bg-green-100', color: 'text-green-600' }
    }
    return { icon: MinusIcon, bg: 'bg-red-100', color: 'text-red-500' }
  }

  const getTypeBadge = (type) => {
    const badges = {
      DEPOSIT: 'bg-green-100 text-green-700',
      WITHDRAWAL: 'bg-orange-100 text-orange-700',
      TRANSFER: 'bg-blue-100 text-blue-700',
      TRANSFER_OUT: 'bg-orange-100 text-orange-700',
      TRANSFER_IN: 'bg-green-100 text-green-700',
      EXCHANGE: 'bg-purple-100 text-purple-700',
      EXCHANGE_OUT: 'bg-orange-100 text-orange-700',
      EXCHANGE_IN: 'bg-green-100 text-green-700',
    }
    return badges[type] || 'bg-gray-100 text-gray-700'
  }

  // Calculate stats
  const stats = {
    income: transactions.filter(tx => tx.amount > 0).reduce((sum, tx) => sum + tx.amount, 0),
    expense: Math.abs(transactions.filter(tx => tx.amount < 0).reduce((sum, tx) => sum + tx.amount, 0)),
    count: transactions.length,
  }
  stats.net = stats.income - stats.expense

  // Pagination
  const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage)
  const paginatedTransactions = filteredTransactions.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  )

  const clearFilters = () => {
    setSearchQuery('')
    setTypeFilter('All Types')
    setAccountFilter('All Accounts')
    setDateFilter('Last 30 days')
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
      {/* Page Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-heading font-bold text-text mb-2">{t('history.title')}</h1>
          <p className="text-text/60">{t('history.subtitle')}</p>
        </div>
        <button className="flex items-center space-x-2 px-5 py-3 bg-white border border-border text-text rounded-xl hover:bg-surface transition-colors duration-200 cursor-pointer">
          <ArrowDownTrayIcon className="w-5 h-5" />
          <span className="font-medium">{t('common.export')}</span>
        </button>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-white rounded-xl p-4 shadow-sm border border-border">
          <p className="text-text/60 text-sm mb-1">{t('history.totalIncome')}</p>
          <p className="text-xl font-heading font-bold text-green-600">+{formatCurrency(stats.income)}</p>
          <p className="text-xs text-text/50 mt-1">{t('history.thisMonthLabel')}</p>
        </div>
        <div className="bg-white rounded-xl p-4 shadow-sm border border-border">
          <p className="text-text/60 text-sm mb-1">{t('history.totalExpense')}</p>
          <p className="text-xl font-heading font-bold text-red-500">-{formatCurrency(stats.expense)}</p>
          <p className="text-xs text-text/50 mt-1">{t('history.thisMonthLabel')}</p>
        </div>
        <div className="bg-white rounded-xl p-4 shadow-sm border border-border">
          <p className="text-text/60 text-sm mb-1">{t('history.transactions')}</p>
          <p className="text-xl font-heading font-bold text-text">{stats.count}</p>
          <p className="text-xs text-text/50 mt-1">{t('history.thisMonthLabel')}</p>
        </div>
        <div className="bg-white rounded-xl p-4 shadow-sm border border-border">
          <p className="text-text/60 text-sm mb-1">{t('history.netChange')}</p>
          <p className={`text-xl font-heading font-bold ${stats.net >= 0 ? 'text-primary' : 'text-red-500'}`}>
            {stats.net >= 0 ? '+' : ''}{formatCurrency(stats.net)}
          </p>
          <p className="text-xs text-text/50 mt-1">{t('history.thisMonthLabel')}</p>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-2xl shadow-sm border border-border p-6 mb-6">
        <div className="flex flex-wrap items-center gap-4">
          {/* Search */}
          <div className="flex-1 min-w-[200px]">
            <div className="relative">
              <MagnifyingGlassIcon className="w-5 h-5 text-text/40 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={t('history.searchTransactions')}
                className="w-full pl-10 pr-4 py-3 bg-surface border border-border rounded-xl text-text placeholder-text/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors duration-200"
              />
            </div>
          </div>

          {/* Type Filter */}
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="px-4 py-3 bg-surface border border-border rounded-xl cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option>{t('history.allTypes')}</option>
            <option>{t('history.types.transfer')}</option>
            <option>{t('history.types.exchange')}</option>
            <option>{t('history.types.deposit')}</option>
            <option>{t('history.types.withdrawal')}</option>
          </select>

          {/* Account Filter */}
          <select
            value={accountFilter}
            onChange={(e) => setAccountFilter(e.target.value)}
            className="px-4 py-3 bg-surface border border-border rounded-xl cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option>{t('history.allAccounts')}</option>
            {accounts.map((acc) => (
              <option key={acc.accountId}>{acc.currency} Account</option>
            ))}
          </select>

          {/* Date Filter */}
          <select
            value={dateFilter}
            onChange={(e) => setDateFilter(e.target.value)}
            className="px-4 py-3 bg-surface border border-border rounded-xl cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option>{t('history.last30Days')}</option>
            <option>{t('history.last7Days')}</option>
            <option>{t('history.thisMonth')}</option>
            <option>{t('history.lastMonth')}</option>
            <option>{t('history.last3Months')}</option>
          </select>
        </div>

        {/* Active Filters */}
        {(typeFilter !== 'All Types' || accountFilter !== 'All Accounts' || searchQuery) && (
          <div className="flex flex-wrap gap-2 mt-4">
            {typeFilter !== 'All Types' && (
              <span className="px-3 py-1 bg-primary/10 text-primary text-sm rounded-full flex items-center">
                {typeFilter}
                <button onClick={() => setTypeFilter('All Types')} className="ml-2 hover:text-primary/70 cursor-pointer">
                  <XMarkIcon className="w-4 h-4" />
                </button>
              </span>
            )}
            {accountFilter !== 'All Accounts' && (
              <span className="px-3 py-1 bg-primary/10 text-primary text-sm rounded-full flex items-center">
                {accountFilter}
                <button onClick={() => setAccountFilter('All Accounts')} className="ml-2 hover:text-primary/70 cursor-pointer">
                  <XMarkIcon className="w-4 h-4" />
                </button>
              </span>
            )}
            {searchQuery && (
              <span className="px-3 py-1 bg-primary/10 text-primary text-sm rounded-full flex items-center">
                "{searchQuery}"
                <button onClick={() => setSearchQuery('')} className="ml-2 hover:text-primary/70 cursor-pointer">
                  <XMarkIcon className="w-4 h-4" />
                </button>
              </span>
            )}
            <button onClick={clearFilters} className="px-3 py-1 text-text/50 text-sm hover:text-primary cursor-pointer">
              {t('common.clearAll')}
            </button>
          </div>
        )}
      </div>

      {/* Transaction List */}
      <div className="bg-white rounded-2xl shadow-sm border border-border overflow-hidden">
        {/* Table Header */}
        <div className="hidden md:grid grid-cols-12 gap-4 px-6 py-4 bg-surface border-b border-border text-sm font-medium text-text/60">
          <div className="col-span-4">{t('history.transaction')}</div>
          <div className="col-span-2">{t('history.type')}</div>
          <div className="col-span-2">{t('history.account')}</div>
          <div className="col-span-2">{t('history.date')}</div>
          <div className="col-span-2 text-right">{t('transfer.amount')}</div>
        </div>

        {/* Transaction Items */}
        <div className="divide-y divide-border">
          {paginatedTransactions.length === 0 ? (
            <div className="px-6 py-12 text-center text-text/50">
              {t('history.noTransactionsFound')}
            </div>
          ) : (
            paginatedTransactions.map((tx) => {
              const typeInfo = getTypeIcon(tx.transactionType || tx.type, tx.amount)
              const IconComponent = typeInfo.icon

              return (
                <div
                  key={tx.transactionId}
                  className="grid grid-cols-1 md:grid-cols-12 gap-4 px-6 py-4 hover:bg-surface transition-colors duration-200 cursor-pointer"
                >
                  <div className="col-span-4 flex items-center space-x-4">
                    <div className={`w-10 h-10 ${typeInfo.bg} rounded-xl flex items-center justify-center`}>
                      <IconComponent className={`w-5 h-5 ${typeInfo.color}`} />
                    </div>
                    <div>
                      <p className="font-medium text-text">{tx.description || tx.transactionType || tx.type}</p>
                      <p className="text-sm text-text/50 md:hidden">
                        {new Date(tx.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                  <div className="col-span-2 flex items-center">
                    <span className={`px-2 py-1 text-xs font-medium rounded-lg ${getTypeBadge(tx.transactionType || tx.type)}`}>
                      {tx.transactionType || tx.type}
                    </span>
                  </div>
                  <div className="col-span-2 flex items-center text-text/70">
                    {tx.accountCurrency} Account
                  </div>
                  <div className="col-span-2 flex items-center text-text/70">
                    {new Date(tx.createdAt).toLocaleDateString()}
                  </div>
                  <div className="col-span-2 flex items-center justify-end">
                    <span className={`font-semibold ${
                      tx.transactionType === 'WITHDRAWAL' || tx.transactionType === 'TRANSFER_OUT' || tx.transactionType === 'EXCHANGE_OUT'
                        ? 'text-red-500'
                        : tx.transactionType === 'DEPOSIT' || tx.transactionType === 'TRANSFER_IN' || tx.transactionType === 'EXCHANGE_IN'
                          ? 'text-green-600'
                          : tx.amount >= 0
                            ? 'text-green-600'
                            : 'text-red-500'
                      }`}>
                      {(tx.transactionType === 'WITHDRAWAL' || tx.transactionType === 'TRANSFER_OUT' || tx.transactionType === 'EXCHANGE_OUT')
                        ? '-'
                        : (tx.transactionType === 'DEPOSIT' || tx.transactionType === 'TRANSFER_IN' || tx.transactionType === 'EXCHANGE_IN')
                          ? '+'
                          : tx.amount >= 0
                            ? '+'
                            : ''}{formatCurrency(Math.abs(tx.amount), tx.currency || tx.accountCurrency)}
                    </span>
                  </div>
                </div>
              )
            })
          )}
        </div>

        {/* Pagination */}
        {filteredTransactions.length > itemsPerPage && (
          <div className="flex items-center justify-between px-6 py-4 border-t border-border">
            <p className="text-sm text-text/60">
              Showing {(currentPage - 1) * itemsPerPage + 1}-{Math.min(currentPage * itemsPerPage, filteredTransactions.length)} of {filteredTransactions.length} transactions
            </p>
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="px-3 py-2 bg-surface border border-border rounded-lg disabled:text-text/30 disabled:cursor-not-allowed hover:bg-primary/5 transition-colors cursor-pointer"
              >
                <ChevronLeftIcon className="w-4 h-4" />
              </button>
              {Array.from({ length: Math.min(3, totalPages) }, (_, i) => i + 1).map((page) => (
                <button
                  key={page}
                  onClick={() => setCurrentPage(page)}
                  className={`px-3 py-2 rounded-lg font-medium cursor-pointer transition-colors ${currentPage === page
                    ? 'bg-primary text-white'
                    : 'bg-surface border border-border text-text hover:bg-primary/5'
                    }`}
                >
                  {page}
                </button>
              ))}
              <button
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="px-3 py-2 bg-surface border border-border rounded-lg disabled:text-text/30 disabled:cursor-not-allowed hover:bg-primary/5 transition-colors cursor-pointer"
              >
                <ChevronRightIcon className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
