import { useState, useRef, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useNotifications } from '../contexts/NotificationContext'
import {
  BellIcon,
  CheckCircleIcon,
  XCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  TrashIcon
} from '@heroicons/react/24/outline'

export default function NotificationDropdown() {
  const { t } = useTranslation()
  const { notifications, unreadCount, markAllAsRead, clearNotifications, fetchUnreadNotifications } = useNotifications()
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef(null)

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Toggle dropdown - refresh and mark as read when opening
  const handleToggle = () => {
    if (!isOpen) {
      // Refresh notifications when opening dropdown
      fetchUnreadNotifications()
      // Mark all as read when opening (badge clears immediately)
      if (unreadCount > 0) {
        markAllAsRead()
      }
    }
    setIsOpen(!isOpen)
  }

  const getIcon = (type) => {
    switch (type) {
      case 'TRANSFER_SENT':
      case 'TRANSFER_RECEIVED':
      case 'TRANSFER_COMPLETED':
      case 'DEPOSIT_SUCCESS':
      case 'DEPOSIT_COMPLETED':
      case 'WITHDRAWAL_SUCCESS':
      case 'EXCHANGE_SUCCESS':
      case 'EXCHANGE_COMPLETED':
        return <CheckCircleIcon className="w-5 h-5 text-green-500" />
      case 'TRANSFER_FAILED':
      case 'WITHDRAWAL_FAILED':
        return <XCircleIcon className="w-5 h-5 text-red-500" />
      case 'BALANCE_LOW':
        return <ExclamationTriangleIcon className="w-5 h-5 text-yellow-500" />
      default:
        return <InformationCircleIcon className="w-5 h-5 text-blue-500" />
    }
  }

  const formatTime = (date) => {
    if (!date) return ''
    const d = new Date(date)
    const now = new Date()
    const diffMs = now - d
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return t('notification.justNow', '剛剛')
    if (diffMins < 60) return t('notification.minutesAgo', '{{count}} 分鐘前', { count: diffMins })
    if (diffHours < 24) return t('notification.hoursAgo', '{{count}} 小時前', { count: diffHours })
    return t('notification.daysAgo', '{{count}} 天前', { count: diffDays })
  }

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell Button */}
      <button
        onClick={handleToggle}
        className="p-2 rounded-xl hover:bg-primary/5 transition-colors duration-200 cursor-pointer relative"
      >
        <BellIcon className="w-6 h-6 text-text/70" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs font-bold rounded-full flex items-center justify-center">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown Panel */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-border overflow-hidden z-50">
          {/* Header */}
          <div className="px-4 py-3 bg-gray-50 border-b border-border flex items-center justify-between">
            <h3 className="font-semibold text-text">
              {t('notification.title', '通知')}
            </h3>
            {notifications.length > 0 && (
              <button
                onClick={clearNotifications}
                className="text-sm text-gray-500 hover:text-red-500 flex items-center gap-1 transition-colors"
              >
                <TrashIcon className="w-4 h-4" />
                {t('notification.clearAll', '清除全部')}
              </button>
            )}
          </div>

          {/* Notification List */}
          <div className="max-h-80 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="py-12 text-center text-gray-400">
                <BellIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p>{t('notification.empty', '暫無通知')}</p>
              </div>
            ) : (
              <ul className="divide-y divide-border">
                {notifications.map((notification) => (
                  <li
                    key={notification.id}
                    className={`px-4 py-3 hover:bg-gray-50 transition-colors ${
                      !notification.read ? 'bg-blue-50/50' : ''
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex-shrink-0 mt-0.5">
                        {getIcon(notification.type)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-text truncate">
                          {notification.title}
                        </p>
                        <p className="text-sm text-gray-500 mt-0.5">
                          {notification.message}
                        </p>
                        <p className="text-xs text-gray-400 mt-1">
                          {formatTime(notification.createdAt)}
                        </p>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
