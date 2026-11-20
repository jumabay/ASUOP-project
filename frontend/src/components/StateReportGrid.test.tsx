import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConfigProvider } from 'antd'
import { describe, expect, it, vi } from 'vitest'
import type { StateReportLine } from '../types'
import { StateReportGrid } from './StateReportGrid'

const sampleLines: StateReportLine[] = [
  {
    reportId: 1001,
    wagonCode: 'SYN-00001',
    dislocationTime: '2024-01-15T08:00:00',
    stationCode: '6900',
    operationCode: '01',
  },
  {
    reportId: 1002,
    wagonCode: 'SYN-00001',
    dislocationTime: '2024-01-15T09:00:00',
    stationCode: '6901',
    operationCode: '02',
  },
]

describe('StateReportGrid', () => {
  it('shows empty day Empty and 0 / max 500 caption', () => {
    render(
      <ConfigProvider>
        <StateReportGrid lines={[]} empty lineCount={0} />
      </ConfigProvider>,
    )
    expect(screen.getByText('За выбранные сутки записей нет')).toBeInTheDocument()
    expect(screen.getByTestId('day-report-line-count')).toHaveTextContent('0 / max 500')
  })

  it('shows line count caption when data loaded', () => {
    render(
      <ConfigProvider>
        <StateReportGrid lines={sampleLines} empty={false} lineCount={sampleLines.length} />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('day-report-line-count')).toHaveTextContent('2 / max 500')
  })

  it('shows loading via DayReportStatus', () => {
    render(
      <ConfigProvider>
        <StateReportGrid lines={[]} empty={false} loading />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('day-report-loading')).toBeInTheDocument()
    expect(screen.queryByText('За выбранные сутки записей нет')).not.toBeInTheDocument()
  })

  it('shows error with retry', async () => {
    const onRetry = vi.fn()
    const user = userEvent.setup()
    render(
      <ConfigProvider>
        <StateReportGrid lines={[]} empty={false} error="Временная ошибка" onRetry={onRetry} />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('day-report-error')).toBeInTheDocument()
    await user.click(screen.getByTestId('day-report-retry'))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
