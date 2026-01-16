import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import Navbar from './Navbar'

export default function Layout() {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="min-h-screen bg-surface">
      <Navbar />
      <main className="pt-28 pb-8 px-4">
        <div className="max-w-7xl mx-auto">
          <Outlet />
        </div>
      </main>
      <footer className="py-6 text-center text-text/40 text-sm">
        <p>Lite Bank Demo - React + Vite + Tailwind</p>
      </footer>
    </div>
  )
}
