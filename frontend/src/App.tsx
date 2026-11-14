import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Flex,
  Input,
  Layout,
  Row,
  Space,
  Spin,
  Typography,
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useState } from 'react'
import { fetchStateReports, fetchWagonCard } from './api/wagonsApi'
import { StateReportGrid } from './components/StateReportGrid'
import { WagonCardPanel } from './components/WagonCardPanel'
import { ApiError, type StateReportDay, type WagonCard } from './types'
import './App.css'

const { Header, Content } = Layout
const { Title, Paragraph, Text } = Typography

export default function App() {
  const [wagonCode, setWagonCode] = useState('SYN-00001')
  const [date, setDate] = useState<Dayjs>(dayjs('2024-01-15'))
  const [loading, setLoading] = useState(false)
  const [card, setCard] = useState<WagonCard | null>(null)
  const [day, setDay] = useState<StateReportDay | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [limitExceeded, setLimitExceeded] = useState(false)

  async function onSearch() {
    const code = wagonCode.trim()
    if (!code || !date) {
      setError('Укажите номер вагона и дату')
      return
    }

    setLoading(true)
    setError(null)
    setNotFound(false)
    setLimitExceeded(false)
    setCard(null)
    setDay(null)

    try {
      const wagonCard = await fetchWagonCard(code)
      setCard(wagonCard)
      const stateDay = await fetchStateReports(code, date.format('YYYY-MM-DD'))
      setDay(stateDay)
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === 'WAGON_NOT_FOUND') {
          setNotFound(true)
          setError('Вагон не найден')
        } else if (err.code === 'RESULT_LIMIT_EXCEEDED') {
          setLimitExceeded(true)
          setError('Превышен лимит 500 строк за сутки. Результат не обрезан молча.')
        } else {
          setError(err.message)
        }
      } else {
        setError(err instanceof Error ? err.message : 'Неизвестная ошибка')
      }
    } finally {
      setLoading(false)
    }
  }

  const emptyDay = Boolean(day && day.lines.length === 0 && !error)

  return (
    <Layout className="app-shell">
      <Header className="app-header">
        <Flex align="center" justify="space-between">
          <div>
            <Text className="brand">АСУВП</Text>
            <Title level={4} className="header-title">
              Карточка и дислокация вагона
            </Title>
          </div>
          <Text type="secondary">R1 · synthetic</Text>
        </Flex>
      </Header>
      <Content className="app-content">
        <Card className="search-card">
          <Paragraph type="secondary" style={{ marginBottom: 16 }}>
            Поиск по синтетическому коду (например SYN-00001) и дате суток.
          </Paragraph>
          <Row gutter={[12, 12]} align="middle">
            <Col xs={24} md={10}>
              <Input
                size="large"
                placeholder="Номер вагона"
                value={wagonCode}
                onChange={(e) => setWagonCode(e.target.value)}
                onPressEnter={() => void onSearch()}
                allowClear
              />
            </Col>
            <Col xs={24} md={8}>
              <DatePicker
                size="large"
                style={{ width: '100%' }}
                value={date}
                onChange={(value) => value && setDate(value)}
                format="YYYY-MM-DD"
              />
            </Col>
            <Col xs={24} md={6}>
              <Button type="primary" size="large" block loading={loading} onClick={() => void onSearch()}>
                Найти
              </Button>
            </Col>
          </Row>
        </Card>

        <Space orientation="vertical" size="large" style={{ width: '100%', marginTop: 24 }}>
          {loading && (
            <Flex justify="center" style={{ padding: 32 }}>
              <Spin size="large" description="Загрузка…" />
            </Flex>
          )}

          {notFound && <Alert type="warning" showIcon title="Вагон не найден" />}
          {limitExceeded && (
            <Alert
              type="error"
              showIcon
              title="RESULT_LIMIT_EXCEEDED"
              description="Строк больше 500. Сервер вернул HTTP 422 без обрезки данных."
            />
          )}
          {error && !notFound && !limitExceeded && (
            <Alert type="error" showIcon title={error} />
          )}

          {!loading && card && (
            <Card>
              <WagonCardPanel card={card} />
            </Card>
          )}

          {!loading && day && (
            <Card title={`История дислокации · ${day.date}`}>
              <StateReportGrid lines={day.lines} empty={emptyDay} />
            </Card>
          )}
        </Space>
      </Content>
    </Layout>
  )
}
