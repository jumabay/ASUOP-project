import { render, screen, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { WatchedMembershipBanner } from '../components/WatchedMembershipBanner'
import { WatchedWagonGrid } from '../components/WatchedWagonGrid'
import { WagonCardPanel } from '../components/WagonCardPanel'

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('P5-D watched membership honesty', () => {
  it('renders FIXTURE demo banner', () => {
    render(
      <ConfigProvider>
        <WatchedMembershipBanner
          source="FIXTURE"
          note="Демонстрационный synthetic-список (FIXTURE). G31 OPEN."
        />
      </ConfigProvider>,
    )
    const banner = screen.getByTestId('watched-membership-banner')
    expect(banner).toHaveAttribute('data-membership-source', 'FIXTURE')
    expect(banner).toHaveTextContent(/FIXTURE/)
    expect(banner).toHaveTextContent(/демо/i)
  })

  it('renders EMPTY_PENDING_G31 explanation banner', () => {
    render(
      <ConfigProvider>
        <WatchedMembershipBanner
          source="EMPTY_PENDING_G31"
          note="Подтверждённый источник отсутствует (G31 OPEN)."
        />
      </ConfigProvider>,
    )
    const banner = screen.getByTestId('watched-membership-banner')
    expect(banner).toHaveAttribute('data-membership-source', 'EMPTY_PENDING_G31')
    expect(banner).toHaveTextContent(/G31/)
  })

  it('WatchedWagonGrid shows FIXTURE banner and filter-empty copy', async () => {
    render(
      <ConfigProvider>
        <WatchedWagonGrid
          rows={[]}
          empty
          onSelect={() => undefined}
          membershipSource="FIXTURE"
          membershipNote="FIXTURE demo note"
        />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('watched-membership-banner')).toHaveAttribute(
      'data-membership-source',
      'FIXTURE',
    )
    expect(screen.getByText('По заданным условиям вагоны не найдены')).toBeInTheDocument()
  })

  it('WatchedWagonGrid shows G31 empty explanation for EMPTY_PENDING_G31', () => {
    render(
      <ConfigProvider>
        <WatchedWagonGrid
          rows={[]}
          empty
          onSelect={() => undefined}
          membershipSource="EMPTY_PENDING_G31"
          membershipNote="G31 OPEN — источник отсутствует"
        />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('watched-membership-banner')).toHaveAttribute(
      'data-membership-source',
      'EMPTY_PENDING_G31',
    )
    const empty = screen.getByTestId('watched-empty-pending-g31')
    expect(empty).toBeInTheDocument()
    expect(empty).toHaveTextContent(/G31 OPEN/)
  })

  it('WatchedWagonGrid self-fetches membershipSource when prop omitted', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        jsonResponse({
          content: [],
          page: 0,
          size: 1,
          totalElements: 0,
          totalPages: 0,
          membershipSource: 'FIXTURE',
          membershipNote: 'FIXTURE from API',
        }),
      ),
    )
    render(
      <ConfigProvider>
        <WatchedWagonGrid rows={[]} empty onSelect={() => undefined} />
      </ConfigProvider>,
    )
    await waitFor(() => {
      expect(screen.getByTestId('watched-membership-banner')).toHaveAttribute(
        'data-membership-source',
        'FIXTURE',
      )
    })
  })

  it('wagon card tag is navigation context, not Active membership', () => {
    render(
      <ConfigProvider>
        <WagonCardPanel
          fromWatched
          card={{
            wagonCode: 'SYN-00001',
            model: '12-132',
            wagonKind: 'КРЫТЫЙ',
            ownerName: 'SYN-OWNER',
            homeStationCode: '6900',
            homeStationName: 'SYN-STATION',
            inventoryPark: true,
          }}
        />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('wagon-card-watched-tag')).toHaveTextContent('Контекст: слежение')
    expect(screen.queryByText('На слежении')).not.toBeInTheDocument()
  })
})
