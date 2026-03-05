import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 3001,
    proxy: {
      // SSE endpoint - connect directly to notification-service (bypass API Gateway)
      '/api/notifications': {
        target: 'http://localhost:8089',
        changeOrigin: true,
        // SSE specific settings - disable all timeouts
        timeout: 0,
        proxyTimeout: 0,
        ws: true,
      },
      // Regular API endpoints
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
    },
  },
})
