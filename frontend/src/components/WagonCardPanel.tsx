import { Button, Descriptions, Empty, Flex, Space, Tag, Tooltip, Typography } from 'antd'
import type { WagonCard } from '../types'

const { Text } = Typography

interface Props {
  card: WagonCard | null
  /** Client hint when card was opened from the watched tab — not G31 membership parity. */
  fromWatched?: boolean
}

export function WagonCardPanel({ card, fromWatched = false }: Props) {
  if (!card) {
    return <Empty description="Карточка вагона не загружена" />
  }

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
            {fromWatched && <Tag color="blue">На слежении</Tag>}
          </Flex>
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
