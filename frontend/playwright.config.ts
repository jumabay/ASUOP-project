import { defineConfig, devices } from '@playwright/test'

const previewPort = Number(process.env.PW_PREVIEW_PORT ?? '4173')
const baseURL = process.env.PW_BASE_URL ?? `http://127.0.0.1:${previewPort}`

/**
 * P1-4 browser smoke against synthetic/offline API.
 * Backend must already listen (default proxy target http://localhost:8080).
 * Alternate API port: set API_PROXY_TARGET before build/preview (see SYNTHETIC_RUNTIME_SMOKE.md).
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  outputDir: 'test-results',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
    locale: 'ru-RU',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], channel: undefined },
    },
  ],
  webServer: {
    command: `npm run preview -- --host 127.0.0.1 --port ${previewPort}`,
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
