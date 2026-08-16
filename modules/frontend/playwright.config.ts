import { defineConfig } from '@playwright/test'
import { existsSync, readdirSync } from 'node:fs'

/**
 * End-to-end tests against a real browser (see docs/prompt/tests.adoc).
 *
 * <p>By default Playwright starts the **Boot jar** – the artifact that is actually shipped,
 * including the SPA packaged into it – so the tests exercise the same single-origin setup as
 * production. With `E2E_BASE_URL` (Gradle: `-Pe2e.baseUrl=...`) the suite runs against an
 * already deployed instance instead and starts nothing.
 *
 * <p>The application needs a reachable PostgreSQL, exactly like `bootRun`.
 *
 * <p>All artefacts – videos of every run, plus traces and screenshots of failures – are written
 * to `backend/bootstrap/build/e2e/`, together with the HTML report that links them.
 */
const externalBaseUrl = process.env.E2E_BASE_URL?.trim()
const port = Number(process.env.E2E_PORT ?? 8080)
/** Origin only – the context path (docs/prompt/frontend.adoc) is part of the paths in the specs. */
const baseURL = externalBaseUrl || `http://localhost:${port}`
/** Must match server.servlet.context-path of the backend. */
export const CONTEXT_PATH = '/recipes'

/** Relative to this config: the build dir of :backend:bootstrap (see docs/prompt/architektur.adoc). */
const ARTEFACT_DIR = '../backend/bootstrap/build/e2e'

function bootJar(): string {
  const libs = '../backend/bootstrap/build/libs'
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
  // All artefacts land under the Gradle build directory, so `gradle clean` removes them and
  // nothing needs an extra .gitignore entry.
  outputDir: `${ARTEFACT_DIR}/test-results`,
  // Always write the HTML report – locally it is just as useful as in CI. `open: 'never'`
  // keeps it from launching a browser on a failed run.
  reporter: [['list'], ['html', { open: 'never', outputFolder: `${ARTEFACT_DIR}/report` }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    // Every run is recorded, not just failures: the videos document the test execution itself.
    // The size must match the viewport below, otherwise the page is letterboxed into the frame.
    video: { mode: 'on', size: { width: 1280, height: 800 } },
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
          // Wait for the API rather than the SPA – it is the last thing to become ready.
          url: `${baseURL}${CONTEXT_PATH}/api/recipes`,
          reuseExistingServer: !process.env.CI,
          timeout: 120_000,
        },
      }),
})
