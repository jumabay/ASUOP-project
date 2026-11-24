import { Alert } from 'antd'
import type { WatchedMembershipSource } from '../types'

interface Props {
  source: WatchedMembershipSource
  note: string
}

/**
 * Honesty banner for inventory-park membership (P5-G31 / stubs).
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
        title="Демо-список (FIXTURE)"
        description={
          note ||
          'Synthetic fixture — не инвентарный парк из Oracle, не персональное слежение. G31 OPEN.'
        }
      />
    )
  }

  if (source === 'DBTC_ACTIVE_INVENTORY_PARK') {
    return (
      <Alert
        type="success"
        showIcon
        data-testid="watched-membership-banner"
        data-membership-source="DBTC_ACTIVE_INVENTORY_PARK"
        style={{ marginBottom: 12 }}
        title="Инвентарный парк (Active / GetActiveWagonList membership)"
        description={
          note ||
          'Членство инвентарного парка (dbtc-ro). Не персональное слежение; UI-паритет со старым экраном «На слежении» не подтверждён.'
        }
      />
    )
  }

  if (source === 'SOURCE_NOT_CONNECTED') {
    return (
      <Alert
        type="warning"
        showIcon
        data-testid="watched-membership-banner"
        data-membership-source="SOURCE_NOT_CONNECTED"
        style={{ marginBottom: 12 }}
        title="Источник не подключён"
        description={
          note ||
          'JDBC для списков registry/watched не утверждён. Пустой список не означает отсутствие вагонов.'
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
      title="Источник инвентарного парка не подтверждён (G31 OPEN)"
      description={
        note ||
        'Подтверждённый источник членства инвентарного парка отсутствует. Список пуст до утверждения источника.'
      }
    />
  )
}