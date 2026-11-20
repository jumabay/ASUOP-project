import { Alert, Button } from 'antd'

export interface ListErrorRetryProps {
  message: string
  onRetry: () => void
}

/** Transient list error (registry / watched) with explicit retry. */
export function ListErrorRetry({ message, onRetry }: ListErrorRetryProps) {
  return (
    <Alert
      type="error"
      showIcon
      title={message}
      data-testid="list-error-retry"
      action={
        <Button size="small" type="primary" onClick={onRetry} data-testid="list-error-retry-button">
          Повторить
        </Button>
      }
    />
  )
}
