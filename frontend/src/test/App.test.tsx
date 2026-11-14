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

describe('App R1 scenarios', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shows wagon card and history table', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/state-reports')) {
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
      return jsonResponse({
        wagonCode: 'SYN-00001',
        model: '12-132',
        wagonKind: 'КРЫТЫЙ',
        ownerName: 'SYN-OWNER',
        homeStationCode: '6900',
        homeStationName: 'SYN-STATION',
        inventoryPark: true,
      })
    })

    const user = userEvent.setup()
    render(
      <ConfigProvider>
        <App />
      </ConfigProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Найти' }))

    await waitFor(() => {
      expect(screen.getByText('12-132')).toBeInTheDocument()
    })
    expect(screen.getByText('История дислокации · 2024-01-15')).toBeInTheDocument()
  })

  it('shows not found message', async () => {
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse({ error: 'WAGON_NOT_FOUND', message: 'Wagon not found' }, 404),
    )

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
  })

  it('shows empty day state', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockImplementation(async (input) => {
      const url = String(input)
      if (url.includes('/state-reports')) {
        return jsonResponse({ wagonCode: 'SYN-00001', date: '2024-01-16', lines: [] })
      }
      return jsonResponse({
        wagonCode: 'SYN-00001',
        model: '12-132',
        wagonKind: 'КРЫТЫЙ',
        ownerName: 'SYN-OWNER',
        homeStationCode: '6900',
        homeStationName: 'SYN-STATION',
        inventoryPark: true,
      })
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
  })

  it('shows limit exceeded error', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockImplementation(async (input) => {
      const url = String(input)
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
      return jsonResponse({
        wagonCode: 'SYN-00001',
        model: '12-132',
        wagonKind: 'КРЫТЫЙ',
        ownerName: 'SYN-OWNER',
        homeStationCode: '6900',
        homeStationName: 'SYN-STATION',
        inventoryPark: true,
      })
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
  })
})
