import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConfigProvider } from 'antd'
import { describe, expect, it, vi } from 'vitest'
import { ListErrorRetry } from './ListErrorRetry'

describe('ListErrorRetry', () => {
  it('renders message and invokes onRetry', async () => {
    const onRetry = vi.fn()
    const user = userEvent.setup()
    render(
      <ConfigProvider>
        <ListErrorRetry message="Ошибка загрузки реестра" onRetry={onRetry} />
      </ConfigProvider>,
    )

    expect(screen.getByTestId('list-error-retry')).toBeInTheDocument()
    expect(screen.getByText('Ошибка загрузки реестра')).toBeInTheDocument()

    await user.click(screen.getByTestId('list-error-retry-button'))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
