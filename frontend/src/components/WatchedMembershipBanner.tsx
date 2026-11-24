import { Alert } from 'antd'
import type { WatchedMembershipSource } from '../types'

interface Props {
  source: WatchedMembershipSource
  note: string
}

/**
 * Honesty banner for watched membership (P5-D). Lives inside watched UI —
 * does not require App.tsx changes when the grid resolves source itself.
 */
export function WatchedMembershipBanner({ source, note }: Props) {
  if (source === 'FIXTURE') {
    return (
      <Alert
        type="warning"
        showIcon
        data-testid="watched-membership-banner"
        data-membership-source="FIXTURE"
        style={{ marginBottom: 12 }}
        title="Демо-список слежения (FIXTURE)"
        description={
          note ||
          'Synthetic fixture — не подтверждённый активный список слежения компании. G31 OPEN.'
        }
      />
    )
  }

  return (
    <Alert
      type="info"
      showIcon
      data-testid="watched-membership-banner"
      data-membership-source="EMPTY_PENDING_G31"
      style={{ marginBottom: 12 }}
      title="Источник слежения не подтверждён (G31 OPEN)"
      description={
        note ||
        'Подтверждённый источник членства отсутствует. Список пуст до утверждения источника.'
      }
    />
  )
}
