import axios from 'axios'
import { registerAxiosTracing } from '../tracing/instrumentation/axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// 註冊 OpenTelemetry 追蹤攔截器（需在其他攔截器之前）
registerAxiosTracing(api)

// Request interceptor - add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor - handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Auth API
export const authAPI = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  logout: () => api.post('/auth/logout'),
}

// Account API
export const accountAPI = {
  getAll: () => api.get('/accounts'),
  getById: (id) => api.get(`/accounts/${id}`),
  getByUserId: (userId) => api.get('/accounts', { params: { userId } }),
  getBalance: (accountId) => api.get(`/accounts/${accountId}/balance`),
  getPublicInfo: (accountNumber) => api.get(`/accounts/number/${accountNumber}`),
  create: (data) => api.post('/accounts', data),
}

// Recipient API
export const recipientAPI = {
  getByUserId: (userId) => api.get('/recipients', { params: { userId } }),
  create: (data) => api.post('/recipients', data),
  delete: (recipientId, userId) => api.delete(`/recipients/${recipientId}`, { params: { userId } }),
}

// Transaction API
export const transactionAPI = {
  getAll: (params = {}) => api.get('/transactions', { params }),
  getByAccountId: (accountId, params = {}) => api.get('/transactions', { params: { accountId, ...params } }),
  getById: (transactionId) => api.get(`/transactions/${transactionId}`),
  getByTraceId: (traceId) => api.get(`/transactions/trace/${traceId}`),
}

// Transfer API
export const transferAPI = {
  create: (data) => api.post('/transfers', data),
}

// Exchange Rate API
export const rateAPI = {
  getRate: (from, to) => api.get(`/rates/${from}/${to}`),
  getCurrencies: () => api.get('/rates/currencies'),
  convert: (from, to, amount) => api.get('/rates/convert', { params: { from, to, amount } }),
}

// Exchange API
export const exchangeAPI = {
  getRates: () => rateAPI.getCurrencies(), // Backward compatible alias
  getRate: (from, to) => rateAPI.getRate(from, to),
  exchange: (data) => api.post('/exchanges', data),
}

// Deposit/Withdraw API
export const depositWithdrawAPI = {
  deposit: (data) => api.post('/deposits', data),
  withdraw: (data) => api.post('/withdrawals', data),
}

// Analytics API
export const analyticsAPI = {
  getSummary: () => api.get('/analytics/summary'),
  getIncomeExpense: (months = 6) => api.get('/analytics/income-expense', { params: { months } }),
  getTrends: (months = 6) => api.get('/analytics/trends', { params: { months } }),
  getDistribution: () => api.get('/analytics/distribution'),
}

export default api
