import { Routes, Route } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { NotificationProvider } from './contexts/NotificationContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import Transfer from './pages/Transfer'
import Exchange from './pages/Exchange'
import History from './pages/History'
import Toast from './components/Toast'

function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <Toast />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<Layout />}>
            <Route index element={<Dashboard />} />
            <Route path="accounts" element={<Accounts />} />
            <Route path="transfer" element={<Transfer />} />
            <Route path="exchange" element={<Exchange />} />
            <Route path="history" element={<History />} />
          </Route>
        </Routes>
      </NotificationProvider>
    </AuthProvider>
  )
}

export default App
