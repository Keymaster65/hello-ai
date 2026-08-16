import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

/**
 * The Spring Boot application is the BFF: in production it serves this bundle from its own
 * origin under the context path `/recipes` (docs/prompt/frontend.adoc), so the app only ever calls paths
 * relative to `import.meta.env.BASE_URL`. During development Vite serves under the same base
 * and proxies the backend paths, which keeps the code identical in both modes.
 */
export default defineConfig({
  plugins: [react()],
  // Must match server.servlet.context-path of the backend – it becomes BASE_URL in the bundle.
  base: '/recipes/',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    // Only the backend paths are proxied, not `/recipes/` itself – otherwise the proxy would
    // swallow the SPA that Vite serves under the same base.
    proxy: {
      '/recipes/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/recipes/v3': { target: 'http://localhost:8080', changeOrigin: true },
      '/recipes/swagger-ui': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
