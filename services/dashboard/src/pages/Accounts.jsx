import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { accountAPI, transactionAPI } from '../services/api'
import { formatCurrency } from '../utils/formatCurrency'
import {
  ChevronDownIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  ArrowsRightLeftIcon,
  PlusIcon,
  XMarkIcon,
  CheckCircleIcon
} from '@heroicons/react/24/outline'

export default function Accounts() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [selectedAccount, setSelectedAccount] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [dropdownOpen, setDropdownOpen] = useState(false)

  // Create account modal state
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [selectedCurrency, setSelectedCurrency] = useState('')
  const [isCreating, setIsCreating] = useState(false)
  const [createResult, setCreateResult] = useState(null) // { success: boolean, account?: object, error?: string }

  // Supported currencies
  const allCurrencies = ['TWD', 'USD', 'EUR', 'JPY', 'GBP']

  useEffect(() => {
    loadAccounts()
  }, [user])

  const loadAccounts = async () => {
    try {
      const res = await accountAPI.getByUserId(user?.userId || 1)
      const accountList = res.data.data || []
      setAccounts(accountList)

      // 預設選擇 TWD 帳戶，若無則選第一個
      const twdAccount = accountList.find(acc => acc.currency === 'TWD')
      const defaultAccount = twdAccount || accountList[0]
      if (defaultAccount) {
        selectAccount(defaultAccount)
      }
    } catch (error) {
      console.error('Failed to load accounts:', error)
    } finally {
      setLoading(false)
    }
  }

  const selectAccount = async (account) => {
    setSelectedAccount(account)
    setDropdownOpen(false)
    try {
      const res = await transactionAPI.getByAccountId(account.accountId)
      const txData = res.data.data
      setTransactions(txData?.content || txData || [])
    } catch (error) {
      console.error('Failed to load transactions:', error)
    }
  }

  const getCurrencyIcon = (currency) => {
    const icons = {
      USD: { symbol: '$', bg: 'bg-cta/10', text: 'text-cta', name: '美元帳戶' },
      EUR: { symbol: '€', bg: 'bg-purple-100', text: 'text-purple-600', name: '歐元帳戶' },
      TWD: { symbol: 'NT', bg: 'bg-orange-100', text: 'text-orange-600', name: '新台幣帳戶' },
      JPY: { symbol: '¥', bg: 'bg-red-100', text: 'text-red-600', name: '日圓帳戶' },
      GBP: { symbol: '£', bg: 'bg-blue-100', text: 'text-blue-600', name: '英鎊帳戶' },
    }
    return icons[currency] || { symbol: currency, bg: 'bg-gray-100', text: 'text-gray-600', name: `${currency} 帳戶` }
  }

  // Get currencies that user doesn't have yet
  const existingCurrencies = accounts.map(acc => acc.currency)
  const availableCurrencies = allCurrencies.filter(c => !existingCurrencies.includes(c))

  // Create account handlers
  const openCreateModal = () => {
    setSelectedCurrency(availableCurrencies[0] || '')
    setCreateResult(null)
    setShowCreateModal(true)
  }

  const closeCreateModal = () => {
    setShowCreateModal(false)
    setCreateResult(null)
  }

  const handleCreateAccount = async () => {
    if (!selectedCurrency) return

    setIsCreating(true)
    try {
      const res = await accountAPI.create({
        userId: user?.userId || 1,
        currency: selectedCurrency
      })
      const newAccount = res.data.data
      setCreateResult({ success: true, account: newAccount })
      // Reload accounts list
      await loadAccounts()
      // Select the new account
      if (newAccount) {
        selectAccount(newAccount)
      }
    } catch (error) {
      console.error('Failed to create account:', error)
      setCreateResult({
        success: false,
        error: error.response?.data?.message || error.message || t('accounts.createError')
      })
    } finally {
      setIsCreating(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    )
  }

  const selectedIcon = selectedAccount ? getCurrencyIcon(selectedAccount.currency) : null

  return (
    <div className="max-w-3xl mx-auto">
      {/* Page Header */}
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="text-3xl font-heading font-bold text-text mb-2">{t('accounts.title')}</h1>
          <p className="text-text/60">{t('accounts.subtitle')}</p>
        </div>
        {availableCurrencies.length > 0 && (
          <button
            onClick={openCreateModal}
            className="flex items-center space-x-2 px-4 py-2 bg-primary text-white rounded-xl hover:bg-primary/90 transition-colors cursor-pointer"
          >
            <PlusIcon className="w-5 h-5" />
            <span>{t('accounts.createAccount')}</span>
          </button>
        )}
      </div>

      {/* Account Selector Dropdown */}
      <div className="relative mb-8">
        <button
          onClick={() => setDropdownOpen(!dropdownOpen)}
          className="w-full flex items-center justify-between px-5 py-4 bg-white rounded-xl border border-border hover:border-primary/50 transition-colors duration-200 cursor-pointer"
        >
          {selectedAccount && (
            <div className="flex items-center space-x-3">
              <div className={`w-10 h-10 ${selectedIcon.bg} rounded-lg flex items-center justify-center`}>
                <span className={`font-bold ${selectedIcon.text}`}>{selectedIcon.symbol}</span>
              </div>
              <div className="text-left">
                <p className="font-medium text-text">{t(`accounts.currencyNames.${selectedAccount.currency}`, selectedIcon.name)}</p>
                <p className="text-sm text-text/50">{selectedAccount.currency}</p>
              </div>
            </div>
          )}
          <ChevronDownIcon className={`w-5 h-5 text-text/50 transition-transform duration-200 ${dropdownOpen ? 'rotate-180' : ''}`} />
        </button>

        {/* Dropdown Menu */}
        {dropdownOpen && (
          <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl border border-border shadow-lg z-10 overflow-hidden">
            {accounts.map((account) => {
              const icon = getCurrencyIcon(account.currency)
              const isSelected = selectedAccount?.accountId === account.accountId
              return (
                <button
                  key={account.accountId}
                  onClick={() => selectAccount(account)}
                  className={`w-full flex items-center justify-between px-5 py-4 hover:bg-surface transition-colors duration-200 cursor-pointer ${isSelected ? 'bg-primary/5' : ''}`}
                >
                  <div className="flex items-center space-x-3">
                    <div className={`w-10 h-10 ${icon.bg} rounded-lg flex items-center justify-center`}>
                      <span className={`font-bold ${icon.text}`}>{icon.symbol}</span>
                    </div>
                    <div className="text-left">
                      <p className="font-medium text-text">{t(`accounts.currencyNames.${account.currency}`, icon.name)}</p>
                      <p className="text-sm text-text/50">{account.currency}</p>
                    </div>
                  </div>
                  {isSelected && (
                    <span className="text-primary text-sm font-medium">✓</span>
                  )}
                </button>
              )
            })}
          </div>
        )}
      </div>

      {/* Selected Account Details */}
      {selectedAccount && (
        <div className="bg-white rounded-2xl shadow-sm border border-border overflow-hidden">
          {/* Account Header */}
          <div className={`${selectedIcon.bg} p-8`}>
            <div className="flex items-center space-x-4 mb-6">
              <div className="w-16 h-16 bg-white/80 rounded-xl flex items-center justify-center">
                <span className={`text-2xl font-bold ${selectedIcon.text}`}>{selectedIcon.symbol}</span>
              </div>
              <div>
                <h2 className="text-xl font-heading font-semibold text-text">
                  {t(`accounts.currencyNames.${selectedAccount.currency}`, selectedIcon.name)}
                </h2>
                <p className="text-text/60 text-sm">
                  {t('accounts.accountNumber')}: {selectedAccount.accountNumber || 'N/A'}
                </p>
              </div>
            </div>
            <div>
              <p className="text-text/60 text-sm mb-1">{t('accounts.availableBalance')}</p>
              <p className="text-4xl font-heading font-bold text-text">
                {formatCurrency(selectedAccount.balance, selectedAccount.currency)}
              </p>
            </div>
          </div>

          {/* Account Info */}
          <div className="grid grid-cols-2 gap-4 p-6 border-b border-border">
            <div>
              <p className="text-text/50 text-sm mb-1">{t('accounts.accountType')}</p>
              <p className="font-medium text-text">{t('accounts.savings')}</p>
            </div>
            <div>
              <p className="text-text/50 text-sm mb-1">{t('accounts.status')}</p>
              <span className="inline-flex items-center px-2 py-1 bg-green-100 text-green-700 text-sm font-medium rounded-full">
                {t('common.active')}
              </span>
            </div>
          </div>

          {/* Recent Transactions */}
          <div className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-heading font-semibold text-text">{t('accounts.recentTransactions')}</h3>
              <a href="/history" className="text-sm text-primary hover:text-primary/80 cursor-pointer">
                {t('common.viewAll')}
              </a>
            </div>

            <div className="space-y-3">
              {transactions.length === 0 ? (
                <p className="text-center text-text/50 py-8">{t('accounts.noTransactions')}</p>
              ) : (
                transactions.slice(0, 5).map((tx) => (
                  <div
                    key={tx.id}
                    className="flex items-center justify-between p-4 rounded-xl bg-surface hover:bg-primary/5 transition-colors duration-200"
                  >
                    <div className="flex items-center space-x-4">
                      <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                        tx.transactionType === 'WITHDRAWAL'
                          ? 'bg-red-100'
                          : tx.transactionType === 'DEPOSIT'
                            ? 'bg-green-100'
                            : tx.transactionType === 'EXCHANGE' || tx.transactionType === 'EXCHANGE_IN' || tx.transactionType === 'EXCHANGE_OUT'
                              ? 'bg-purple-100'
                              : tx.amount > 0
                                ? 'bg-green-100'
                                : 'bg-red-100'
                      }`}>
                        {tx.transactionType === 'WITHDRAWAL' ? (
                          <ArrowDownIcon className="w-5 h-5 text-red-500" />
                        ) : tx.transactionType === 'DEPOSIT' ? (
                          <ArrowUpIcon className="w-5 h-5 text-green-600" />
                        ) : tx.transactionType === 'EXCHANGE' || tx.transactionType === 'EXCHANGE_IN' || tx.transactionType === 'EXCHANGE_OUT' ? (
                          <ArrowsRightLeftIcon className="w-5 h-5 text-purple-600" />
                        ) : tx.amount > 0 ? (
                          <ArrowUpIcon className="w-5 h-5 text-green-600" />
                        ) : (
                          <ArrowDownIcon className="w-5 h-5 text-red-500" />
                        )}
                      </div>
                      <div>
                        <p className="font-medium text-text">{tx.description || tx.transactionType || tx.type}</p>
                        <p className="text-sm text-text/50">
                          {new Date(tx.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    <p className={`font-semibold ${
                      tx.transactionType === 'WITHDRAWAL'
                        ? 'text-red-500'
                        : tx.transactionType === 'DEPOSIT'
                          ? 'text-green-600'
                          : tx.amount > 0
                            ? 'text-green-600'
                            : 'text-red-500'
                    }`}>
                      {tx.transactionType === 'WITHDRAWAL' ? '-' : tx.transactionType === 'DEPOSIT' ? '+' : tx.amount > 0 ? '+' : ''}
                      {formatCurrency(Math.abs(tx.amount), tx.currency)}
                    </p>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* No Account State */}
      {!selectedAccount && !loading && (
        <div className="bg-white rounded-2xl p-12 text-center border border-border">
          <p className="text-text/50">{t('accounts.noAccountsAvailable')}</p>
        </div>
      )}

      {/* Create Account Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl p-6 w-full max-w-md mx-4 shadow-xl">
            {createResult?.success ? (
              /* Success State */
              <>
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-xl font-heading font-semibold text-text">
                    {t('accounts.createSuccess')}
                  </h2>
                  <button
                    onClick={closeCreateModal}
                    className="p-2 hover:bg-surface rounded-lg transition-colors"
                  >
                    <XMarkIcon className="w-5 h-5 text-text/60" />
                  </button>
                </div>

                <div className="text-center py-4">
                  <div className="w-16 h-16 mx-auto rounded-full flex items-center justify-center bg-green-100">
                    <CheckCircleIcon className="w-10 h-10 text-green-600" />
                  </div>
                  <p className="mt-3 text-lg font-medium text-text">{t('accounts.accountCreated')}</p>
                </div>

                <div className="bg-surface rounded-xl p-4 space-y-3 mb-4">
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{t('accounts.currency')}</span>
                    <span className="font-heading font-semibold text-text">
                      {createResult.account?.currency}
                    </span>
                  </div>
                  <div className="border-t border-border"></div>
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{t('accounts.accountNumber')}</span>
                    <span className="font-medium text-text">
                      {createResult.account?.accountNumber}
                    </span>
                  </div>
                  <div className="border-t border-border"></div>
                  <div className="flex justify-between items-center">
                    <span className="text-text/60">{t('accounts.initialBalance')}</span>
                    <span className="font-medium text-text">
                      {formatCurrency(0, createResult.account?.currency)}
                    </span>
                  </div>
                </div>

                <button
                  onClick={closeCreateModal}
                  className="w-full py-3 rounded-xl font-medium transition-colors bg-primary text-white hover:bg-primary/90"
                >
                  {t('common.done')}
                </button>
              </>
            ) : createResult?.success === false ? (
              /* Error State */
              <>
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-xl font-heading font-semibold text-red-700">
                    {t('accounts.createFailed')}
                  </h2>
                  <button
                    onClick={closeCreateModal}
                    className="p-2 hover:bg-surface rounded-lg transition-colors"
                  >
                    <XMarkIcon className="w-5 h-5 text-text/60" />
                  </button>
                </div>

                <div className="text-center py-4">
                  <div className="w-16 h-16 mx-auto rounded-full flex items-center justify-center bg-red-100">
                    <XMarkIcon className="w-10 h-10 text-red-500" />
                  </div>
                  <p className="mt-3 text-lg font-medium text-red-700">{createResult.error}</p>
                </div>

                <div className="flex gap-3">
                  <button
                    onClick={() => setCreateResult(null)}
                    className="flex-1 py-3 rounded-xl font-medium transition-colors border border-primary text-primary hover:bg-primary/5"
                  >
                    {t('errors.tryAgain')}
                  </button>
                  <button
                    onClick={closeCreateModal}
                    className="flex-1 py-3 rounded-xl font-medium transition-colors bg-primary text-white hover:bg-primary/90"
                  >
                    {t('common.close')}
                  </button>
                </div>
              </>
            ) : (
              /* Form State */
              <>
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-xl font-heading font-semibold text-text">
                    {t('accounts.createAccount')}
                  </h2>
                  <button
                    onClick={closeCreateModal}
                    className="p-2 hover:bg-surface rounded-lg transition-colors"
                  >
                    <XMarkIcon className="w-5 h-5 text-text/60" />
                  </button>
                </div>

                <div className="mb-6">
                  <label className="block text-sm font-medium text-text/70 mb-2">
                    {t('accounts.selectCurrency')}
                  </label>
                  <div className="space-y-2">
                    {availableCurrencies.map((currency) => {
                      const icon = getCurrencyIcon(currency)
                      const isSelected = selectedCurrency === currency
                      return (
                        <button
                          key={currency}
                          onClick={() => setSelectedCurrency(currency)}
                          className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border-2 transition-colors cursor-pointer ${
                            isSelected
                              ? 'border-primary bg-primary/5'
                              : 'border-border hover:border-primary/50'
                          }`}
                        >
                          <div className="flex items-center space-x-3">
                            <div className={`w-10 h-10 ${icon.bg} rounded-lg flex items-center justify-center`}>
                              <span className={`font-bold ${icon.text}`}>{icon.symbol}</span>
                            </div>
                            <div className="text-left">
                              <p className="font-medium text-text">{t(`accounts.currencyNames.${currency}`, icon.name)}</p>
                              <p className="text-sm text-text/50">{currency}</p>
                            </div>
                          </div>
                          {isSelected && (
                            <span className="text-primary font-medium">✓</span>
                          )}
                        </button>
                      )
                    })}
                  </div>
                </div>

                <button
                  onClick={handleCreateAccount}
                  disabled={isCreating || !selectedCurrency}
                  className="w-full py-3 rounded-xl font-medium transition-colors bg-primary text-white hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isCreating ? t('common.processing') : t('accounts.confirmCreate')}
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
