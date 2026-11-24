import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConfigProvider } from 'antd'
import { describe, expect, it, vi } from 'vitest'
import { DAY_LINE_MAX, DayReportStatus } from './DayReportStatus'

describe('DayReportStatus', () => {
  it('shows loading state', () => {
    render(
      <ConfigProvider>
        <DayReportStatus loading />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('day-report-loading')).toBeInTheDocument()
    expect(screen.getByText('Загрузка суток…')).toBeInTheDocument()
  })

  it('shows error and calls onRetry', async () => {
    const onRetry = vi.fn()
    const user = userEvent.setup()
    render(
      <ConfigProvider>
        <DayReportStatus error="Сеть недоступна" onRetry={onRetry} />
      </ConfigProvider>,
    )

    expect(screen.getByTestId('day-report-error')).toBeInTheDocument()
    expect(screen.getByText('Сеть недоступна')).toBeInTheDocument()

    await user.click(screen.getByTestId('day-report-retry'))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })

  it('shows line count caption against max 500', () => {
    render(
      <ConfigProvider>
        <DayReportStatus lineCount={12} />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('day-report-line-count')).toHaveTextContent(
      `12 / max ${DAY_LINE_MAX}`,
    )
  })

  it('shows zero count caption for empty day', () => {
    render(
      <ConfigProvider>
        <DayReportStatus lineCount={0} />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('day-report-line-count')).toHaveTextContent('0 / max 500')
  })

  it('prefers loading over error and caption', () => {
    render(
      <ConfigProvider>
        <DayReportStatus loading error="boom" lineCount={3} />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('day-report-loading')).toBeInTheDocument()
    expect(screen.queryByTestId('day-report-error')).not.toBeInTheDocument()
    expect(screen.queryByTestId('day-report-line-count')).not.toBeInTheDocument()
  })
})
