import { render, screen, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { WatchedMembershipBanner } from '../components/WatchedMembershipBanner'
import { WagonRegistryGrid } from '../components/WagonRegistryGrid'

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('platform runtime honesty (offline mocks)', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    window.history.replaceState(null, '', '/')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    window.history.replaceState(null, '', '/')
  })

  it('shows backend displayLabel instead of hardcoded synthetic badge', async () => {
    vi.mocked(fetch).mockImplementation(async (input) => {
      const path = String(input).split('?')[0]
      if (path.includes('/platform/runtime')) {
        return jsonResponse({
          dataSourceKind: 'DBTC_RO',
          displayLabel: 'DBTC read-only (time-boxed)',
          registryConnected: false,
          watchedConnected: false,
        })
      }
      if (path.endsWith('/wagons/watched')) {
        return jsonResponse({
          content: [],
          page: 0,
          size: 50,
          totalElements: 0,
          totalPages: 0,
          membershipSource: 'SOURCE_NOT_CONNECTED',
          membershipNote: 'Источник не подключён.',
        })
      }
      return jsonResponse({
        content: [],
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 0,
      })
    })

    render(
      <ConfigProvider>
        <App />
      </ConfigProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('data-source-label')).toHaveTextContent(
        'DBTC read-only (time-boxed)',
      )
    })
    expect(screen.queryByText('R3 · synthetic')).toBeNull()
    await waitFor(() => {
      expect(
        screen.getAllByText(/Источник не подключён/).length,
      ).toBeGreaterThan(0)
    })
  })

  it('banner and registry empty use Источник не подключён copy', () => {
    render(
      <ConfigProvider>
        <WatchedMembershipBanner
          source="SOURCE_NOT_CONNECTED"
          note="Источник не подключён. JDBC для списков не утверждён."
        />
        <WagonRegistryGrid
          rows={[]}
          empty
          emptyDescription="Источник не подключён"
          onSelect={() => undefined}
        />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('watched-membership-banner')).toHaveAttribute(
      'data-membership-source',
      'SOURCE_NOT_CONNECTED',
    )
    expect(screen.getByTestId('registry-empty')).toHaveTextContent('Источник не подключён')
  })
})
