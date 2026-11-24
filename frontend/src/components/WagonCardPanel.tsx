import { Button, Descriptions, Empty, Flex, Space, Tag, Tooltip, Typography } from 'antd'
import type { PlatformRuntime, WagonCard } from '../types'
import { displayBoolean, displayValue } from '../utils/displayValue'

const { Text } = Typography

const CURRENT_TIME_LABEL_SYNTHETIC = 'Время дислокации'
const CURRENT_TIME_LABEL_DBTC_RO = 'Дата положения по данным источника'
const CURRENT_TIME_HINT_DBTC_RO =
  'Значение из WAGON.LOCATIONDATE (источник Oracle). Назначение «факт дислокации» не подтверждено (G3/G4 / DT1 OPEN). Не ось суток GETTING_DATE.'

interface Props {
  card: WagonCard | null
  /** Client navigation hint when card was opened from the watched tab — not G31 / Active membership. */
  fromWatched?: boolean
  /** Runtime honesty; drives dbtc-ro current-time label. */
  dataSourceKind?: PlatformRuntime['dataSourceKind'] | null
}

export function currentStateTimeLabel(dataSourceKind?: string | null): string {
  return dataSourceKind === 'DBTC_RO' ? CURRENT_TIME_LABEL_DBTC_RO : CURRENT_TIME_LABEL_SYNTHETIC
}

export function WagonCardPanel({ card, fromWatched = false, dataSourceKind = null }: Props) {
  if (!card) {
    return <Empty description="Карточка вагона не загружена" />
  }

  const current = card.currentState
  const timeLabel = currentStateTimeLabel(dataSourceKind)
  const dbtcRo = dataSourceKind === 'DBTC_RO'

  return (
    <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
      <Descriptions
        bordered
        size="small"
        column={2}
        title={
          <Flex align="center" gap={8} wrap>
            <span>
              Карточка вагона <Text code>{card.wagonCode}</Text>
            </span>
            {fromWatched && (
              <Tooltip title="Контекст навигации: карточка открыта из вкладки «Вагоны на слежении». Не подтверждает Active / GetActiveWagonList членство (G31 OPEN).">
                <Tag color="blue" data-testid="wagon-card-watched-tag">
                  Контекст: слежение
                </Tag>
              </Tooltip>
            )}
          </Flex>
        }
      >
        <Descriptions.Item label="Модель">{displayValue(card.model)}</Descriptions.Item>
        <Descriptions.Item label="Род вагона">{displayValue(card.wagonKind)}</Descriptions.Item>
        <Descriptions.Item label="Собственник">{displayValue(card.ownerName)}</Descriptions.Item>
        <Descriptions.Item label="Инвентарный парк">
          {displayBoolean(card.inventoryPark)}
        </Descriptions.Item>
        <Descriptions.Item label="Станция приписки" span={2}>
          {displayValue(card.homeStationCode)} — {displayValue(card.homeStationName)}
        </Descriptions.Item>
      </Descriptions>

      <div data-testid="wagon-card-current-state">
        <Descriptions bordered size="small" column={2} title="Текущее состояние">
          {current == null ? (
            <Descriptions.Item label="Данные" span={2}>
              {displayValue(null)}
            </Descriptions.Item>
          ) : (
            <>
              <Descriptions.Item label="Станция">
                {displayValue(current.stationCode)}
              </Descriptions.Item>
              <Descriptions.Item label="Операция">
                {displayValue(current.operationCode)}
              </Descriptions.Item>
              <Descriptions.Item
                label={
                  dbtcRo ? (
                    <Tooltip title={CURRENT_TIME_HINT_DBTC_RO}>
                      <span data-testid="wagon-card-current-time-label">{timeLabel}</span>
                    </Tooltip>
                  ) : (
                    <span data-testid="wagon-card-current-time-label">{timeLabel}</span>
                  )
                }
              >
                {displayValue(current.dislocationTime)}
              </Descriptions.Item>
              <Descriptions.Item label="Индекс поезда">
                {displayValue(current.trainIndex)}
              </Descriptions.Item>
            </>
          )}
        </Descriptions>
      </div>

      <Flex gap={8} wrap>
        <Tooltip title="Появится после утверждения модуля">
          <Button disabled>Ремонты</Button>
        </Tooltip>
        <Tooltip title="Появится после утверждения модуля">
          <Button disabled>Склад</Button>
        </Tooltip>
      </Flex>
    </Space>
  )
}
