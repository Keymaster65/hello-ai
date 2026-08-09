import { defineConfig } from '@playwright/test'
import { existsSync, readdirSync } from 'node:fs'

/**
 * End-to-end tests against a real browser (see ADR 0008).
 *
 * <p>By default Playwright starts the **Boot jar** – the artifact that is actually shipped,
 * including the SPA packaged into it – so the tests exercise the same single-origin setup as
 * production. With `E2E_BASE_URL` (Gradle: `-Pe2e.baseUrl=...`) the suite runs against an
 * already deployed instance instead and starts nothing.
 *
 * <p>The application needs a reachable PostgreSQL, exactly like `bootRun`.
 */
const externalBaseUrl = process.env.E2E_BASE_URL?.trim()
const port = Number(process.env.E2E_PORT ?? 8080)
const baseURL = externalBaseUrl || `http://localhost:${port}`

function bootJar(): string {
  const libs = '../build/libs'
  const jar = existsSync(libs)
    ? readdirSync(libs).find((file) => file.endsWith('.jar') && !file.endsWith('-plain.jar'))
    : undefined

  if (!jar) {
    throw new Error(`No Boot jar found in ${libs} – run "./gradlew bootJar" first.`)
  }
  return `${libs}/${jar}`
}

export default defineConfig({
  testDir: './e2e',
  // One shared database, so tests must not race each other.
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  // Always write the HTML report to playwright-report/ – locally it is just as useful as in CI.
  // `open: 'never'` keeps it from launching a browser on a failed run.
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium', viewport: { width: 1280, height: 800 } },
    },
  ],
  ...(externalBaseUrl
    ? {}
    : {
        webServer: {
          command: `java -jar ${bootJar()} --server.port=${port}`,
          // Wait for the API rather than "/" – it is the last thing to become ready.
          url: `${baseURL}/api/recipes`,
          reuseExistingServer: !process.env.CI,
          timeout: 120_000,
        },
      }),
})
