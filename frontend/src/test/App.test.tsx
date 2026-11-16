import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConfigProvider } from 'antd'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'

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

describe('App R1/R2/R3 scenarios', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it(
    'shows watched list by default and opens card from search',
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

      await user.click(screen.getByRole('button', { name: 'Найти' }))

      await waitFor(() => {
        expect(screen.getByText(/История дислокации · 2024-01-15/)).toBeInTheDocument()
      })
    },
    15000,
  )

  it(
    'shows registry tab',
    async () => {
      mockApi()
      const user = userEvent.setup()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await user.click(screen.getByRole('tab', { name: 'Реестр вагонов' }))

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
      await user.click(screen.getByRole('button', { name: 'Найти' }))

      await waitFor(() => {
        expect(screen.getByText('Вагон не найден')).toBeInTheDocument()
      })
    },
    15000,
  )

  it(
    'shows empty day state',
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

      await user.click(screen.getByRole('button', { name: 'Найти' }))

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

      await user.click(screen.getByRole('button', { name: 'Найти' }))

      await waitFor(() => {
        expect(screen.getByText('RESULT_LIMIT_EXCEEDED')).toBeInTheDocument()
      })
    },
    15000,
  )

  it(
    'shows empty watched state',
    async () => {
      mockApi()
      render(
        <ConfigProvider>
          <App />
        </ConfigProvider>,
      )

      await waitFor(() => {
        expect(screen.getByText('По заданным условиям вагоны не найдены')).toBeInTheDocument()
      })
    },
    15000,
  )
})
