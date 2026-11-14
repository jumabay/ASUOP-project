import { Descriptions, Empty, Typography } from 'antd'
import type { WagonCard } from '../types'

const { Text } = Typography

interface Props {
  card: WagonCard | null
}

export function WagonCardPanel({ card }: Props) {
  if (!card) {
    return <Empty description="Карточка вагона не загружена" />
  }

  return (
    <Descriptions
      bordered
      size="small"
      column={2}
      title={
        <span>
          Карточка вагона <Text code>{card.wagonCode}</Text>
        </span>
      }
    >
      <Descriptions.Item label="Модель">{card.model}</Descriptions.Item>
      <Descriptions.Item label="Род вагона">{card.wagonKind}</Descriptions.Item>
      <Descriptions.Item label="Собственник">{card.ownerName}</Descriptions.Item>
      <Descriptions.Item label="Инвентарный парк">
        {card.inventoryPark ? 'Да' : 'Нет'}
      </Descriptions.Item>
      <Descriptions.Item label="Станция приписки" span={2}>
        {card.homeStationCode} — {card.homeStationName}
      </Descriptions.Item>
    </Descriptions>
  )
}
