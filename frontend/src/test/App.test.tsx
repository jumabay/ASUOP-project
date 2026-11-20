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
}

function mockApi(handler?: (url: string) => Response | undefined) {
  vi.mocked(fetch).mockImplementation(async (input) => {
    const url = String(input)
    const custom = handler?.(url)
    if (custom) {
      return custom
    }
    const path = url.split('?')[0]
    if (path.includes('/state-reports')) {
      return jsonResponse({
        wagonCode: 'SYN-00001',
        date: '2024-01-15',
        lines: [
          {
            reportId: 1001,
            wagonCode: 'SYN-00001',
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
})
