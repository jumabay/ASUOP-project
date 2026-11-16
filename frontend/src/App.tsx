import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Flex,
  Input,
  Layout,
  Pagination,
  Row,
  Select,
  Space,
  Spin,
  Tabs,
  Typography,
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useCallback, useEffect, useState } from 'react'
import {
  fetchStateReports,
  fetchWagonCard,
  fetchWagonRegistry,
  fetchWatchedWagons,
} from './api/wagonsApi'
import { StateReportGrid } from './components/StateReportGrid'
import { WagonCardPanel } from './components/WagonCardPanel'
import { WagonRegistryGrid } from './components/WagonRegistryGrid'
import { WatchedWagonGrid } from './components/WatchedWagonGrid'
import {
  ApiError,
  type StateReportDay,
  type WagonCard,
  type WagonRegistryItem,
  type WatchedWagonItem,
} from './types'
import './App.css'

const { Header, Content } = Layout
const { Title, Paragraph, Text } = Typography

type ListTab = 'registry' | 'watched'

export default function App() {
  const [activeTab, setActiveTab] = useState<ListTab>('watched')
  const [wagonCode, setWagonCode] = useState('SYN-00001')
  const [date, setDate] = useState<Dayjs>(dayjs('2024-01-15'))
  const [loading, setLoading] = useState(false)
  const [card, setCard] = useState<WagonCard | null>(null)
  const [day, setDay] = useState<StateReportDay | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [limitExceeded, setLimitExceeded] = useState(false)

  const [registrySearch, setRegistrySearch] = useState('')
  const [registrySort, setRegistrySort] = useState<'code,asc' | 'code,desc'>('code,asc')
  const [registryPage, setRegistryPage] = useState(0)
  const [registrySize, setRegistrySize] = useState(50)
  const [registryRows, setRegistryRows] = useState<WagonRegistryItem[]>([])
  const [registryTotal, setRegistryTotal] = useState(0)
  const [registryLoading, setRegistryLoading] = useState(false)
  const [registryError, setRegistryError] = useState<string | null>(null)

  const [watchedSearch, setWatchedSearch] = useState('')
  const [watchedSort, setWatchedSort] = useState<'code,asc' | 'code,desc'>('code,asc')
  const [watchedPage, setWatchedPage] = useState(0)
  const [watchedSize, setWatchedSize] = useState(50)
  const [watchedRows, setWatchedRows] = useState<WatchedWagonItem[]>([])
  const [watchedTotal, setWatchedTotal] = useState(0)
  const [watchedLoading, setWatchedLoading] = useState(false)
  const [watchedError, setWatchedError] = useState<string | null>(null)

  const loadRegistry = useCallback(async () => {
    setRegistryLoading(true)
    setRegistryError(null)
    try {
      const page = await fetchWagonRegistry({
        page: registryPage,
        size: registrySize,
        sort: registrySort,
        search: registrySearch,
      })
      setRegistryRows(page.content)
      setRegistryTotal(page.totalElements)
    } catch (err) {
      setRegistryRows([])
      setRegistryTotal(0)
      if (err instanceof ApiError) {
        setRegistryError(err.message)
      } else {
        setRegistryError(err instanceof Error ? err.message : 'Ошибка загрузки реестра')
      }
    } finally {
      setRegistryLoading(false)
    }
  }, [registryPage, registrySize, registrySort, registrySearch])

  const loadWatched = useCallback(async () => {
    setWatchedLoading(true)
    setWatchedError(null)
    try {
      const page = await fetchWatchedWagons({
        page: watchedPage,
        size: watchedSize,
        sort: watchedSort,
        search: watchedSearch,
      })
      setWatchedRows(page.content)
      setWatchedTotal(page.totalElements)
    } catch (err) {
      setWatchedRows([])
      setWatchedTotal(0)
      if (err instanceof ApiError) {
        setWatchedError(err.message)
      } else {
        setWatchedError(err instanceof Error ? err.message : 'Ошибка загрузки списка слежения')
      }
    } finally {
      setWatchedLoading(false)
    }
  }, [watchedPage, watchedSize, watchedSort, watchedSearch])

  useEffect(() => {
    if (activeTab === 'registry') {
      void loadRegistry()
    }
  }, [activeTab, loadRegistry])

  useEffect(() => {
    if (activeTab === 'watched') {
      void loadWatched()
    }
  }, [activeTab, loadWatched])

  async function loadWagonDetails(code: string) {
    const trimmed = code.trim()
    if (!trimmed || !date) {
      setError('Укажите номер вагона и дату')
      return
    }

    setLoading(true)
    setError(null)
    setNotFound(false)
    setLimitExceeded(false)
    setCard(null)
    setDay(null)
    setWagonCode(trimmed)

    try {
      const wagonCard = await fetchWagonCard(trimmed)
      setCard(wagonCard)
      const stateDay = await fetchStateReports(trimmed, date.format('YYYY-MM-DD'))
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
  const registryEmpty = !registryLoading && registryRows.length === 0 && !registryError
  const watchedEmpty = !watchedLoading && watchedRows.length === 0 && !watchedError

  return (
    <Layout className="app-shell">
      <Header className="app-header">
        <Flex align="center" justify="space-between">
          <div>
            <Text className="brand">АСУВП</Text>
            <Title level={4} className="header-title">
              Реестр, слежение и карточка вагона
            </Title>
          </div>
          <Text type="secondary">R3 · synthetic</Text>
        </Flex>
      </Header>
      <Content className="app-content">
        <Card className="search-card">
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as ListTab)}
            items={[
              {
                key: 'watched',
                label: 'Вагоны на слежении',
                children: (
                  <>
                    <Paragraph type="secondary" style={{ marginBottom: 16 }}>
                      Synthetic-подмножество каталога (60 вагонов). Паспорт A2, серверная пагинация.
                      Клик открывает карточку R1. Соответствие GetActiveWagonList не подтверждено.
                    </Paragraph>
                    <Row gutter={[12, 12]} align="middle">
                      <Col xs={24} md={10}>
                        <Input
                          size="large"
                          placeholder="Поиск по номеру (contains)"
                          value={watchedSearch}
                          onChange={(e) => {
                            setWatchedPage(0)
                            setWatchedSearch(e.target.value)
                          }}
                          allowClear
                        />
                      </Col>
                      <Col xs={24} md={6}>
                        <Select
                          size="large"
                          style={{ width: '100%' }}
                          value={watchedSort}
                          onChange={(value: 'code,asc' | 'code,desc') => {
                            setWatchedPage(0)
                            setWatchedSort(value)
                          }}
                          options={[
                            { value: 'code,asc', label: 'Код ↑' },
                            { value: 'code,desc', label: 'Код ↓' },
                          ]}
                        />
                      </Col>
                      <Col xs={24} md={8}>
                        <Button
                          size="large"
                          block
                          loading={watchedLoading}
                          onClick={() => void loadWatched()}
                        >
                          Обновить список
                        </Button>
                      </Col>
                    </Row>
                    <div style={{ marginTop: 16 }}>
                      {watchedLoading && (
                        <Flex justify="center" style={{ padding: 24 }}>
                          <Spin size="large" description="Загрузка слежения…" />
                        </Flex>
                      )}
                      {watchedError && <Alert type="error" showIcon title={watchedError} />}
                      {!watchedLoading && (
                        <WatchedWagonGrid
                          rows={watchedRows}
                          empty={watchedEmpty}
                          onSelect={(code) => void loadWagonDetails(code)}
                        />
                      )}
                      <Flex justify="end" style={{ marginTop: 12 }}>
                        <Pagination
                          current={watchedPage + 1}
                          pageSize={watchedSize}
                          total={watchedTotal}
                          showSizeChanger
                          pageSizeOptions={['50', '100', '200']}
                          onChange={(page, pageSize) => {
                            setWatchedPage(page - 1)
                            setWatchedSize(pageSize)
                          }}
                        />
                      </Flex>
                    </div>
                  </>
                ),
              },
              {
                key: 'registry',
                label: 'Реестр вагонов',
                children: (
                  <>
                    <Paragraph type="secondary" style={{ marginBottom: 16 }}>
                      Серверная пагинация (до 200), поиск contains по номеру, сортировка по коду. Клик
                      по строке открывает карточку R1.
                    </Paragraph>
                    <Row gutter={[12, 12]} align="middle">
                      <Col xs={24} md={10}>
                        <Input
                          size="large"
                          placeholder="Поиск по номеру (contains)"
                          value={registrySearch}
                          onChange={(e) => {
                            setRegistryPage(0)
                            setRegistrySearch(e.target.value)
                          }}
                          allowClear
                        />
                      </Col>
                      <Col xs={24} md={6}>
                        <Select
                          size="large"
                          style={{ width: '100%' }}
                          value={registrySort}
                          onChange={(value: 'code,asc' | 'code,desc') => {
                            setRegistryPage(0)
                            setRegistrySort(value)
                          }}
                          options={[
                            { value: 'code,asc', label: 'Код ↑' },
                            { value: 'code,desc', label: 'Код ↓' },
                          ]}
                        />
                      </Col>
                      <Col xs={24} md={8}>
                        <Button
                          size="large"
                          block
                          loading={registryLoading}
                          onClick={() => void loadRegistry()}
                        >
                          Обновить реестр
                        </Button>
                      </Col>
                    </Row>
                    <div style={{ marginTop: 16 }}>
                      {registryLoading && (
                        <Flex justify="center" style={{ padding: 24 }}>
                          <Spin size="large" description="Загрузка реестра…" />
                        </Flex>
                      )}
                      {registryError && <Alert type="error" showIcon title={registryError} />}
                      {!registryLoading && (
                        <WagonRegistryGrid
                          rows={registryRows}
                          empty={registryEmpty}
                          onSelect={(code) => void loadWagonDetails(code)}
                        />
                      )}
                      <Flex justify="end" style={{ marginTop: 12 }}>
                        <Pagination
                          current={registryPage + 1}
                          pageSize={registrySize}
                          total={registryTotal}
                          showSizeChanger
                          pageSizeOptions={['50', '100', '200']}
                          onChange={(page, pageSize) => {
                            setRegistryPage(page - 1)
                            setRegistrySize(pageSize)
                          }}
                        />
                      </Flex>
                    </div>
                  </>
                ),
              },
            ]}
          />
        </Card>

        <Card className="search-card" style={{ marginTop: 24 }} title="Карточка и история">
          <Paragraph type="secondary" style={{ marginBottom: 16 }}>
            Поиск по синтетическому коду и дате суток (R1).
          </Paragraph>
          <Row gutter={[12, 12]} align="middle">
            <Col xs={24} md={10}>
              <Input
                size="large"
                placeholder="Номер вагона"
                value={wagonCode}
                onChange={(e) => setWagonCode(e.target.value)}
                onPressEnter={() => void loadWagonDetails(wagonCode)}
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
              <Button
                type="primary"
                size="large"
                block
                loading={loading}
                onClick={() => void loadWagonDetails(wagonCode)}
              >
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
          {error && !notFound && !limitExceeded && <Alert type="error" showIcon title={error} />}

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
