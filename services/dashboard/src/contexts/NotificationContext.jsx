import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react'
import { useAuth } from './AuthContext'
import api from '../services/api'

const NotificationContext = createContext(null)

export function NotificationProvider({ children }) {
  const { token, isAuthenticated } = useAuth()
  const [notifications, setNotifications] = useState([])
  const [toastNotifications, setToastNotifications] = useState([]) // Only for SSE real-time notifications
  const [connected, setConnected] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(false)

  // Use refs to avoid re-creating SSE connection on every render
  const eventSourceRef = useRef(null)
  const reconnectTimeoutRef = useRef(null)
  const isMountedRef = useRef(true)

  // Add a new notification from SSE (using ref to avoid dependency issues)
  const addNotificationRef = useRef(null)
  addNotificationRef.current = (notification) => {
    const id = notification.notificationId || Date.now().toString()
    const newNotification = { ...notification, id, createdAt: new Date(), read: false }
    // Add to both lists - notifications for dropdown, toastNotifications for Toast
    setNotifications(prev => [newNotification, ...prev.slice(0, 49)])
    setToastNotifications(prev => [newNotification, ...prev.slice(0, 2)]) // Keep max 3 for toast
    setUnreadCount(prev => prev + 1)
  }

  const addNotification = useCallback((notification) => {
    addNotificationRef.current?.(notification)
  }, [])

  // Fetch existing unread notifications from REST API
  const fetchUnreadNotifications = useCallback(async () => {
    if (!isAuthenticated || !token) return

    setLoading(true)
    try {
      const response = await api.get('/notifications/unread')
      const data = response.data
      if (Array.isArray(data)) {
        const mappedNotifications = data.map(n => ({
          ...n,
          id: n.notificationId || n.id,
          createdAt: n.timestamp ? new Date(n.timestamp) : new Date()
        }))
        setNotifications(mappedNotifications)
        setUnreadCount(data.filter(n => !n.read).length)
      }
    } catch (error) {
      console.error('Failed to fetch notifications:', error)
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated, token])

  // Mark all notifications as read
  const markAllAsRead = useCallback(async () => {
    try {
      await api.put('/notifications/read-all')
      setNotifications(prev => prev.map(n => ({ ...n, read: true })))
      setUnreadCount(0)
    } catch (error) {
      console.error('Failed to mark all as read:', error)
    }
  }, [])

  // Remove a notification from dropdown
  const removeNotification = useCallback((id) => {
    setNotifications(prev => prev.filter(n => n.id !== id))
  }, [])

  // Remove a notification from toast (when dismissed or auto-closed)
  const removeToastNotification = useCallback((id) => {
    setToastNotifications(prev => prev.filter(n => n.id !== id))
  }, [])

  // Clear all notifications (delete from database)
  const clearNotifications = useCallback(async () => {
    try {
      await api.delete('/notifications')
      setNotifications([])
      setUnreadCount(0)
    } catch (error) {
      console.error('Failed to clear notifications:', error)
    }
  }, [])

  // Track if initial fetch is done
  const initialFetchDoneRef = useRef(false)

  // Fetch notifications on mount/login (only once per session)
  useEffect(() => {
    if (isAuthenticated && token && !initialFetchDoneRef.current) {
      initialFetchDoneRef.current = true
      fetchUnreadNotifications()
    }
    // Reset when logged out
    if (!isAuthenticated) {
      initialFetchDoneRef.current = false
      setNotifications([])
      setUnreadCount(0)
    }
  }, [isAuthenticated, token, fetchUnreadNotifications])

  // SSE Connection
  useEffect(() => {
    isMountedRef.current = true

    if (!isAuthenticated || !token) {
      setConnected(false)
      return
    }

    // Don't create new connection if one already exists and is open
    if (eventSourceRef.current && eventSourceRef.current.readyState !== EventSource.CLOSED) {
      console.log('SSE connection already exists, skipping')
      return
    }

    const connect = () => {
      // Don't reconnect if component is unmounted
      if (!isMountedRef.current) {
        console.log('Component unmounted, skipping SSE connection')
        return
      }

      // Clear any existing connection
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }

      // Connect directly to notification-service (bypass Vite proxy for SSE)
      // SSE doesn't support custom headers, so we pass token in URL
      const baseUrl = import.meta.env.DEV ? 'http://localhost:8089' : ''
      const url = `${baseUrl}/api/v1/notifications/stream?token=${encodeURIComponent(token)}`
      const eventSource = new EventSource(url)
      eventSourceRef.current = eventSource

      eventSource.onopen = () => {
        if (!isMountedRef.current) return
        console.log('SSE connected')
        setConnected(true)
      }

      eventSource.addEventListener('connected', (event) => {
        if (!isMountedRef.current) return
        console.log('SSE connection confirmed:', event.data)
        setConnected(true)
      })

      eventSource.addEventListener('notification', (event) => {
        if (!isMountedRef.current) return
        try {
          const notification = JSON.parse(event.data)
          console.log('Received notification:', notification)
          addNotificationRef.current?.(notification)
        } catch (e) {
          console.error('Failed to parse notification:', e)
        }
      })

      eventSource.onerror = (error) => {
        if (!isMountedRef.current) return
        console.error('SSE error:', error)
        setConnected(false)
        eventSource.close()
        eventSourceRef.current = null

        // Clear any existing reconnect timeout
        if (reconnectTimeoutRef.current) {
          clearTimeout(reconnectTimeoutRef.current)
        }

        // Reconnect after 5 seconds
        reconnectTimeoutRef.current = setTimeout(() => {
          if (isMountedRef.current) {
            console.log('Attempting SSE reconnection...')
            connect()
          }
        }, 5000)
      }
    }

    connect()

    return () => {
      isMountedRef.current = false
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current)
        reconnectTimeoutRef.current = null
      }
      setConnected(false)
    }
  }, [isAuthenticated, token]) // Removed addNotification from dependencies

  return (
    <NotificationContext.Provider value={{
      notifications,
      toastNotifications,
      connected,
      unreadCount,
      loading,
      addNotification,
      removeNotification,
      removeToastNotification,
      clearNotifications,
      markAllAsRead,
      fetchUnreadNotifications
    }}>
      {children}
    </NotificationContext.Provider>
  )
}

export function useNotifications() {
  const context = useContext(NotificationContext)
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider')
  }
  return context
}
