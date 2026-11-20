import { Alert, Button, Flex, Spin, Typography } from 'antd'

const { Text } = Typography

export const DAY_LINE_MAX = 500

export interface DayReportStatusProps {
  loading?: boolean
  error?: string | null
  lineCount?: number | null
  maxLines?: number
  onRetry?: () => void
}

/**
 * Day (суточные) status: loading, transient error + retry, or N / max 500 caption.
 * Does not change day semantics or API contracts.
 */
export function DayReportStatus({
  loading = false,
  error = null,
  lineCount = null,
  maxLines = DAY_LINE_MAX,
  onRetry,
}: DayReportStatusProps) {
  if (loading) {
    return (
      <Flex justify="center" style={{ padding: 24 }} data-testid="day-report-loading">
        <Spin size="large" description="Загрузка суток…" />
      </Flex>
    )
  }

  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        title={error}
        data-testid="day-report-error"
        action={
          onRetry ? (
            <Button size="small" type="primary" onClick={onRetry} data-testid="day-report-retry">
              Повторить
            </Button>
          ) : undefined
        }
      />
    )
  }

  if (lineCount !== null && lineCount !== undefined) {
    return (
      <Text type="secondary" data-testid="day-report-line-count">
        {lineCount} / max {maxLines}
      </Text>
    )
  }

  return null
}
