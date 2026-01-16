import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { accountAPI, transactionAPI } from '../services/api'
import {
  PlusIcon,
  ChevronRightIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  ArrowsRightLeftIcon
} from '@heroicons/react/24/outline'

export default function Accounts() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [selectedAccount, setSelectedAccount] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadAccounts()
  }, [user])

  const loadAccounts = async () => {
    try {
      const res = await accountAPI.getByUserId(user?.id || 1)
      setAccounts(res.data.data || [])
      if (res.data.data?.length > 0) {
        selectAccount(res.data.data[0])
      }
    } catch (error) {
      console.error('Failed to load accounts:', error)
    } finally {
      setLoading(false)
    }
  }

  const selectAccount = async (account) => {
    setSelectedAccount(account)
    try {
      const res = await transactionAPI.getByAccountId(account.id)
      // Handle paginated response: data.content contains the transactions array
      const txData = res.data.data
      setTransactions(txData?.content || txData || [])
    } catch (error) {
      console.error('Failed to load transactions:', error)
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
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
    }).format(amount)
  }

  const totalBalance = accounts.reduce((sum, acc) => sum + (acc.balance || 0), 0)

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
          <h1 className="text-3xl font-heading font-bold text-text mb-2">My Accounts</h1>
          <p className="text-text/60">Manage your accounts and view balances</p>
        </div>
        <button className="flex items-center space-x-2 px-5 py-3 bg-primary text-white rounded-xl hover:bg-primary/90 transition-colors duration-200 cursor-pointer">
          <PlusIcon className="w-5 h-5" />
          <span className="font-medium">Open New Account</span>
        </button>
      </div>

      {/* Total Balance Summary */}
      <div className="bg-gradient-to-r from-primary to-secondary rounded-2xl p-8 mb-8 text-white">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-white/80 mb-2">Total Balance (All Accounts)</p>
            <p className="text-4xl font-heading font-bold">{formatCurrency(totalBalance)}</p>
            <p className="text-white/60 mt-2 text-sm">{accounts.length} Active Accounts</p>
          </div>
        </div>
      </div>

      {/* Account Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        {accounts.map((account) => {
          const icon = getCurrencyIcon(account.currency)
          const isSelected = selectedAccount?.id === account.id

          return (
            <div
              key={account.id}
              onClick={() => selectAccount(account)}
              className={`bg-white rounded-2xl p-6 shadow-sm border hover:shadow-md transition-all duration-200 cursor-pointer group ${isSelected ? 'border-primary' : 'border-border'
                }`}
            >
              <div className="flex items-start justify-between mb-6">
                <div className="flex items-center space-x-4">
                  <div className={`w-14 h-14 ${icon.bg} rounded-xl flex items-center justify-center`}>
                    <span className={`text-2xl font-bold ${icon.text}`}>{icon.symbol}</span>
                  </div>
                  <div>
                    <h3 className="font-heading font-semibold text-text text-lg">
                      {account.currency} Account
                    </h3>
                    <p className="text-text/50 text-sm">**** {account.accountNumber?.slice(-4) || '0000'}</p>
                  </div>
                </div>
                <span className="px-3 py-1 bg-green-100 text-green-700 text-xs font-medium rounded-full">
                  Active
                </span>
              </div>

              <div className="mb-6">
                <p className="text-text/60 text-sm mb-1">Available Balance</p>
                <p className="text-3xl font-heading font-bold text-text">
                  {formatCurrency(account.balance, account.currency)}
                </p>
              </div>

              <div className="flex items-center justify-between pt-4 border-t border-border">
                <div className="flex space-x-2">
                  <button className="px-4 py-2 bg-primary/10 text-primary rounded-lg text-sm font-medium hover:bg-primary/20 transition-colors duration-200 cursor-pointer">
                    Transfer
                  </button>
                  <button className="px-4 py-2 bg-primary/10 text-primary rounded-lg text-sm font-medium hover:bg-primary/20 transition-colors duration-200 cursor-pointer">
                    History
                  </button>
                </div>
                <ChevronRightIcon className="w-5 h-5 text-text/30 group-hover:text-primary group-hover:translate-x-1 transition-all duration-200" />
              </div>
            </div>
          )
        })}
      </div>

      {/* Account Details Section */}
      {selectedAccount && (
        <div className="bg-white rounded-2xl shadow-sm border border-border overflow-hidden">
          <div className="bg-gradient-to-r from-cta to-cta/80 p-6 text-white">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
                  <span className="text-xl font-bold">
                    {getCurrencyIcon(selectedAccount.currency).symbol}
                  </span>
                </div>
                <div>
                  <h2 className="text-xl font-heading font-semibold">
                    {selectedAccount.currency} Account
                  </h2>
                  <p className="text-white/70 text-sm">
                    Account Number: {selectedAccount.accountNumber || 'N/A'}
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-white/70 text-sm">Available Balance</p>
                <p className="text-2xl font-heading font-bold">
                  {formatCurrency(selectedAccount.balance, selectedAccount.currency)}
                </p>
              </div>
            </div>
          </div>

          {/* Account Info Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 p-6 border-b border-border">
            <div>
              <p className="text-text/50 text-sm mb-1">Account Type</p>
              <p className="font-medium text-text">{selectedAccount.type || 'Savings'}</p>
            </div>
            <div>
              <p className="text-text/50 text-sm mb-1">Currency</p>
              <p className="font-medium text-text">{selectedAccount.currency}</p>
            </div>
            <div>
              <p className="text-text/50 text-sm mb-1">Opened Date</p>
              <p className="font-medium text-text">
                {new Date(selectedAccount.createdAt).toLocaleDateString()}
              </p>
            </div>
            <div>
              <p className="text-text/50 text-sm mb-1">Status</p>
              <p className="font-medium text-green-600">Active</p>
            </div>
          </div>

          {/* Recent Transactions */}
          <div className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-heading font-semibold text-text">Recent Transactions</h3>
              <a href="/history" className="text-sm text-primary hover:text-primary/80 cursor-pointer">
                View All
              </a>
            </div>

            <div className="space-y-3">
              {transactions.length === 0 ? (
                <p className="text-center text-text/50 py-8">No transactions yet</p>
              ) : (
                transactions.slice(0, 4).map((tx) => (
                  <div
                    key={tx.id}
                    className="flex items-center justify-between p-4 rounded-xl bg-surface hover:bg-primary/5 transition-colors duration-200 cursor-pointer"
                  >
                    <div className="flex items-center space-x-4">
                      <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${tx.transactionType === 'WITHDRAWAL'
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
                          <ArrowDownIcon className="w-5 h-5 text-red-500" />
                        ) : tx.transactionType === 'DEPOSIT' ? (
                          <ArrowUpIcon className="w-5 h-5 text-green-600" />
                        ) : tx.transactionType === 'EXCHANGE' ? (
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
                    <div className="text-right">
                      <p className={`font-semibold ${tx.transactionType === 'WITHDRAWAL'
                        ? 'text-red-500'
                        : tx.transactionType === 'DEPOSIT'
                          ? 'text-green-600'
                          : tx.amount > 0
                            ? 'text-green-600'
                            : 'text-red-500'
                        }`}>
                        {tx.transactionType === 'WITHDRAWAL' ? '-' : tx.transactionType === 'DEPOSIT' ? '+' : tx.amount > 0 ? '+' : ''}{formatCurrency(Math.abs(tx.amount), tx.currency)}
                      </p>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
