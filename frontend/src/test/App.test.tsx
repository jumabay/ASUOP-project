import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConfigProvider } from 'antd'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { LIST_SEARCH_DEBOUNCE_MS } from '../utils/listPaging'

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const emptyRegistry = {
  content: [],
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0,
}

const emptyWatched = {
  content: [],
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0,
  membershipSource: 'FIXTURE' as const,
  membershipNote:
    'Демонстрационный synthetic-список (FIXTURE). Это не подтверждённый активный список слежения компании. G31 OPEN.',
}

const sampleCard = {
  wagonCode: 'SYN-00001',
  model: '12-132',
  wagonKind: 'КРЫТЫЙ',
  ownerName: 'SYN-OWNER',
  homeStationCode: '6900',
  homeStationName: 'SYN-STATION',
  inventoryPark: true,
  currentState: {
    stationCode: '7100',
    operationCode: '05',
    dislocationTime: '2024-01-16T06:00:00',
    trainIndex: 'TRN-DIFF',
  },
}

function mockApi(handler?: (url: string) => Response | undefined) {
  vi.mocked(fetch).mockImplementation(async (input) => {
    const url = String(input)
    const custom = handler?.(url)
    if (custom) {
      return custom
    }
    const path = url.split('?')[0]
    if (path.includes('/platform/runtime')) {
      return jsonResponse({
        dataSourceKind: 'SYNTHETIC',
        displayLabel: 'Synthetic (offline)',
        registryConnected: true,
        watchedConnected: true,
      })
    }
    if (path.includes('/state-reports')) {
      return jsonResponse({
        wagonCode: 'SYN-00001',
        date: '2024-01-15',
        lines: [
          {
            reportId: 1001,
            wagonCode: 'SYN-00001',
            snapshotTime: '2024-01-15T08:00:00',
            dislocationTime: '2024-01-15T08:00:00',
            stationCode: '6900',
            operationCode: '01',
          },
        ],
      })
    }
    if (path.endsWith('/wagons/watched')) {
      return jsonResponse(emptyWatched)
    }
    if (/\/api\/v1\/wagons\/[^/]+$/.test(path)) {
      return jsonResponse(sampleCard)
    }
    return jsonResponse(emptyRegistry)
  })
}

function cardSection() {
  return screen.getByText('Карточка и история').closest('.ant-card') as HTMLElement
}

function cardFindButton() {
  return within(cardSection()).getByRole('button', { name: /^Найти$|^loading Найти$/ })
}

describe('App R1/R2/R3 scenarios', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    window.history.replaceState(null, '', '/')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    window.history.replaceState(null, '', '/')
  })

  it(
    'shows watched list by default and opens card without auto day load',
    async () => {
      mockApi()
      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'Вагоны на слежении' })).toHaveAttribute(
          'aria-selected',
          'true',
        )
      })
      expect(screen.getByText('По заданным условиям вагоны не найдены')).toBeInTheDocument()

      await user.click(cardFindButton())

      await waitFor(() => {
        expect(screen.getByText(/Карточка вагона/)).toBeInTheDocument()
      })
      expect(screen.queryByText(/История дислокации/)).not.toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: 'Загрузить сутки' }))

      await waitFor(() => {
        expect(screen.getByText(/История дислокации · 2024-01-15/)).toBeInTheDocument()
      })
    },
    15000,
  )

  it(
    'shows registry tab and pagination showTotal for large totals',
    async () => {
      mockApi((url) => {
        const path = url.split('?')[0]
        if (path.endsWith('/api/v1/wagons') || path.match(/\/api\/v1\/wagons\?/)) {
          return jsonResponse({
            content: [{ wagonCode: 'SYN-00001' }],
            page: 0,
            size: 50,
            totalElements: 18_000,
            totalPages: 360,
          })
        }
        return undefined
      })
      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await user.click(screen.getByRole('tab', { name: 'Реестр вагонов' }))

      await waitFor(() => {
        expect(screen.getByText(/из 18000 · стр\. 1 из 360/)).toBeInTheDocument()
      })
    },
    15000,
  )

  it(
    'debounces registry search and applies immediately on Найти',
    async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      mockApi()
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await user.click(screen.getByRole('tab', { name: 'Реестр вагонов' }))
      const panel = await screen.findByRole('tabpanel', { name: 'Реестр вагонов' })
      const search = within(panel).getByPlaceholderText('Поиск по номеру (contains)')

      await user.type(search, 'SYN')
      const callsBeforeDebounce = vi.mocked(fetch).mock.calls.filter((c) =>
        String(c[0]).includes('/api/v1/wagons?'),
      ).length

      await vi.advanceTimersByTimeAsync(LIST_SEARCH_DEBOUNCE_MS - 50)
      const callsMid = vi.mocked(fetch).mock.calls.filter((c) =>
        String(c[0]).includes('search=SYN'),
      ).length
      expect(callsMid).toBe(0)

      await vi.advanceTimersByTimeAsync(100)
      await waitFor(() => {
        expect(
          vi.mocked(fetch).mock.calls.some((c) => String(c[0]).includes('search=SYN')),
        ).toBe(true)
      })

      await user.clear(search)
      await user.type(search, 'SYN-9')
      await user.click(within(panel).getByRole('button', { name: 'Найти' }))
      await waitFor(() => {
        expect(
          vi.mocked(fetch).mock.calls.some((c) => String(c[0]).includes('search=SYN-9')),
        ).toBe(true)
      })

      expect(callsBeforeDebounce).toBeGreaterThanOrEqual(0)
      vi.useRealTimers()
    },
    20000,
  )

  it(
    'shows not found message',
    async () => {
      mockApi((url) => {
        const path = url.split('?')[0]
        if (/\/api\/v1\/wagons\/[^/]+$/.test(path) && !path.endsWith('/watched')) {
          return jsonResponse({ error: 'WAGON_NOT_FOUND', message: 'Wagon not found' }, 404)
        }
        if (path.includes('/state-reports')) {
          return jsonResponse({ error: 'WAGON_NOT_FOUND', message: 'Wagon not found' }, 404)
        }
        return undefined
      })

      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await user.clear(screen.getByPlaceholderText('Номер вагона'))
      await user.type(screen.getByPlaceholderText('Номер вагона'), 'SYN-UNKNOWN')
      await user.click(cardFindButton())

      await waitFor(() => {
        expect(screen.getByText('Вагон не найден')).toBeInTheDocument()
      })
    },
    15000,
  )

  it(
    'shows empty day state after explicit day load',
    async () => {
      mockApi((url) => {
        if (url.includes('/state-reports')) {
          return jsonResponse({ wagonCode: 'SYN-00001', date: '2024-01-16', lines: [] })
        }
        return undefined
      })

      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await user.click(screen.getByRole('button', { name: 'Загрузить сутки' }))

      await waitFor(() => {
        expect(screen.getByText('За выбранные сутки записей нет')).toBeInTheDocument()
      })
    },
    15000,
  )

  it(
    'shows limit exceeded error',
    async () => {
      mockApi((url) => {
        if (url.includes('/state-reports')) {
          return jsonResponse(
            {
              error: 'RESULT_LIMIT_EXCEEDED',
              message: 'too many',
              wagonCode: 'SYN-00001',
              date: '2024-01-15',
            },
            422,
          )
        }
        return undefined
      })

      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await user.click(screen.getByRole('button', { name: 'Загрузить сутки' }))

      await waitFor(() => {
        expect(screen.getByText('RESULT_LIMIT_EXCEEDED')).toBeInTheDocument()
      })
    },
    15000,
  )

  it(
    'shows empty watched state and watched tag when card opens from watched tab',
    async () => {
      mockApi()
      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await waitFor(() => {
        expect(screen.getByText('По заданным условиям вагоны не найдены')).toBeInTheDocument()
      })
      await waitFor(() => {
        expect(screen.getByTestId('watched-membership-banner')).toHaveAttribute(
          'data-membership-source',
          'FIXTURE',
        )
      })

      await user.click(cardFindButton())
      await waitFor(() => {
        expect(screen.getByTestId('wagon-card-watched-tag')).toHaveTextContent(
          'Контекст: слежение',
        )
      })
      expect(screen.getByRole('button', { name: 'Ремонты' })).toBeDisabled()
      expect(screen.getByRole('button', { name: 'Склад' })).toBeDisabled()
    },
    15000,
  )

  it(
    'restores tab from deep-link query string',
    async () => {
      window.history.replaceState(null, '', '/?tab=registry&wagon=SYN-00001&date=2024-01-15')
      mockApi()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'Реестр вагонов' })).toHaveAttribute(
          'aria-selected',
          'true',
        )
      })
    },
    15000,
  )

  it(
    'renders shell nav with enabled fleet and disabled stubs',
    async () => {
      mockApi()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await waitFor(() => {
        expect(screen.getByTestId('app-sider-brand')).toBeInTheDocument()
      })
      expect(screen.getByRole('menuitem', { name: /Слежение/ })).toBeInTheDocument()
      expect(screen.getByRole('menuitem', { name: /Реестр/ })).toBeInTheDocument()
      expect(screen.getByTestId('nav-stub-repairs')).toBeInTheDocument()
      expect(screen.getByTestId('nav-stub-warehouse')).toBeInTheDocument()
      expect(screen.getByTestId('nav-stub-reports')).toBeInTheDocument()
      expect(screen.getByRole('menuitem', { name: /Ремонты/ })).toHaveAttribute(
        'aria-disabled',
        'true',
      )
    },
    15000,
  )

  it(
    'global header search loads card without day history',
    async () => {
      mockApi()
      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      const global = screen.getByTestId('global-wagon-search')
      const input = within(global).getByRole('textbox', { name: 'Глобальный поиск вагона' })
      await user.clear(input)
      await user.type(input, 'SYN-00001')
      await user.click(within(global).getByRole('button', { name: /^Найти$|^loading Найти$/ }))

      await waitFor(() => {
        expect(screen.getByText(/Карточка вагона/)).toBeInTheDocument()
      })
      expect(screen.queryByText(/История дислокации/)).not.toBeInTheDocument()
      expect(
        vi.mocked(fetch).mock.calls.some((c) => String(c[0]).includes('/state-reports')),
      ).toBe(false)
    },
    15000,
  )

  it(
    'restores list search/owner/kind/page/size/sort from URL on mount',
    async () => {
      window.history.replaceState(
        null,
        '',
        '/?tab=registry&wagon=SYN-00001&date=2024-01-15&search=SYN&owner=SYN-OWN&kind=%D0%9A%D0%A0%D0%AB%D0%A2%D0%AB%D0%99&page=1&size=100&sort=code,desc',
      )
      mockApi()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'Реестр вагонов' })).toHaveAttribute(
          'aria-selected',
          'true',
        )
      })
      const panel = screen.getByRole('tabpanel', { name: 'Реестр вагонов' })
      expect(within(panel).getByPlaceholderText('Поиск по номеру (contains)')).toHaveValue('SYN')
      expect(within(panel).getByPlaceholderText('Собственник (contains)')).toHaveValue('SYN-OWN')
      expect(within(panel).getByPlaceholderText('Род (equals)')).toHaveValue('КРЫТЫЙ')
      await waitFor(() => {
        expect(
          vi.mocked(fetch).mock.calls.some((c) => {
            const url = decodeURIComponent(String(c[0]))
            return (
              url.includes('/api/v1/wagons?') &&
              url.includes('search=SYN') &&
              url.includes('owner=SYN-OWN') &&
              url.includes('kind=КРЫТЫЙ') &&
              url.includes('page=1') &&
              url.includes('size=100') &&
              url.includes('sort=code,desc')
            )
          }),
        ).toBe(true)
      })
      const preview = decodeURIComponent(
        screen.getByTestId('deep-link-preview').textContent ?? '',
      )
      expect(preview).toContain('search=SYN')
      expect(preview).toContain('owner=SYN-OWN')
      expect(preview).toContain('kind=КРЫТЫЙ')
      expect(preview).toContain('page=1')
      expect(preview).toContain('size=100')
      expect(preview).toContain('sort=code,desc')
    },
    15000,
  )

  it(
    'restores tab from browser popstate (back)',
    async () => {
      mockApi()
      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'Вагоны на слежении' })).toHaveAttribute(
          'aria-selected',
          'true',
        )
      })

      await user.click(screen.getByRole('menuitem', { name: /Реестр/ }))
      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'Реестр вагонов' })).toHaveAttribute(
          'aria-selected',
          'true',
        )
      })
      expect(window.location.search).toContain('tab=registry')

      window.history.back()
      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'Вагоны на слежении' })).toHaveAttribute(
          'aria-selected',
          'true',
        )
      })
    },
    15000,
  )
})
