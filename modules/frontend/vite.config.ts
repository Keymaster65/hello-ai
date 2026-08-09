import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

/**
 * The Spring Boot application is the BFF: in production it serves this bundle from its own
 * origin, so the app only ever calls relative `/api` paths. During development Vite proxies
 * those same paths to the locally running backend, which keeps the code identical in both modes.
 */
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/v3': { target: 'http://localhost:8080', changeOrigin: true },
      '/swagger-ui': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
