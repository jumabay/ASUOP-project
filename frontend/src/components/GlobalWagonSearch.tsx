import { Button, Input, Space } from 'antd'
import { useState } from 'react'

export interface GlobalWagonSearchProps {
  /** Initial value shown in the header search (e.g. current wagon code). */
  initialCode?: string
  loading?: boolean
  onSearch: (code: string) => void
}

/**
 * Header global search: Enter / «Найти» loads the wagon card only (no day history).
 */
export function GlobalWagonSearch({
  initialCode = '',
  loading = false,
  onSearch,
}: GlobalWagonSearchProps) {
  const [code, setCode] = useState(initialCode)

  function submit() {
    onSearch(code)
  }

  return (
    <Space.Compact className="global-wagon-search" data-testid="global-wagon-search">
      <Input
        size="large"
        placeholder="Глобальный поиск вагона"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        onPressEnter={submit}
        allowClear
        aria-label="Глобальный поиск вагона"
        style={{ minWidth: 200, maxWidth: 280 }}
      />
      <Button type="primary" size="large" loading={loading} onClick={submit}>
        Найти
      </Button>
    </Space.Compact>
  )
}
