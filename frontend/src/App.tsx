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
import { useCallback, useEffect, useMemo, useState } from 'react'
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
import {
  buildDeepLinkSearch,
  hasWagonDeepLink,
  parseListTab,
  readDeepLinkFromSearch,
  writeDeepLinkToUrl,
  type ListTab,
} from './utils/deepLink'
import {
  formatListShowTotal,
  LIST_PAGE_SIZE_OPTIONS,
  LIST_SEARCH_DEBOUNCE_MS,
} from './utils/listPaging'
import './App.css'

const { Header, Content } = Layout
const { Title, Paragraph, Text } = Typography

function initialDeepLink() {
  if (typeof window === 'undefined') {
    return { tab: 'watched' as ListTab, wagon: 'SYN-00001', date: '2024-01-15' }
  }
  return readDeepLinkFromSearch(window.location.search)
}

export default function App() {
  const [activeTab, setActiveTab] = useState<ListTab>(() => initialDeepLink().tab)
  const [wagonCode, setWagonCode] = useState(() => initialDeepLink().wagon)
  const [date, setDate] = useState<Dayjs>(() => dayjs(initialDeepLink().date))
  const [cardLoading, setCardLoading] = useState(false)
  const [dayLoading, setDayLoading] = useState(false)
  const [card, setCard] = useState<WagonCard | null>(null)
  const [day, setDay] = useState<StateReportDay | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [dayError, setDayError] = useState<string | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [limitExceeded, setLimitExceeded] = useState(false)
  const [fromWatched, setFromWatched] = useState(() => initialDeepLink().tab === 'watched')

  const [registrySearchInput, setRegistrySearchInput] = useState('')
  const [registrySearch, setRegistrySearch] = useState('')
  const [registrySort, setRegistrySort] = useState<'code,asc' | 'code,desc'>('code,asc')
  const [registryPage, setRegistryPage] = useState(0)
  const [registrySize, setRegistrySize] = useState(50)
  const [registryRows, setRegistryRows] = useState<WagonRegistryItem[]>([])
  const [registryTotal, setRegistryTotal] = useState(0)
  const [registryLoading, setRegistryLoading] = useState(false)
  const [registryError, setRegistryError] = useState<string | null>(null)

  const [watchedSearchInput, setWatchedSearchInput] = useState('')
  const [watchedSearch, setWatchedSearch] = useState('')
  const [watchedSort, setWatchedSort] = useState<'code,asc' | 'code,desc'>('code,asc')
  const [watchedPage, setWatchedPage] = useState(0)
  const [watchedSize, setWatchedSize] = useState(50)
  const [watchedRows, setWatchedRows] = useState<WatchedWagonItem[]>([])
  const [watchedTotal, setWatchedTotal] = useState(0)
  const [watchedLoading, setWatchedLoading] = useState(false)
  const [watchedError, setWatchedError] = useState<string | null>(null)

  const deepLinkState = useMemo(
    () => ({
      tab: activeTab,
      wagon: wagonCode.trim() || 'SYN-00001',
      date: date.format('YYYY-MM-DD'),
    }),
    [activeTab, wagonCode, date],
  )

  useEffect(() => {
    writeDeepLinkToUrl(deepLinkState)
  }, [deepLinkState])

  useEffect(() => {
    if (registrySearchInput === registrySearch) {
      return
    }
    const timer = window.setTimeout(() => {
      setRegistrySearch(registrySearchInput)
      setRegistryPage(0)
    }, LIST_SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [registrySearchInput, registrySearch])

  useEffect(() => {
    if (watchedSearchInput === watchedSearch) {
      return
    }
    const timer = window.setTimeout(() => {
      setWatchedSearch(watchedSearchInput)
      setWatchedPage(0)
    }, LIST_SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [watchedSearchInput, watchedSearch])

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

  const applyRegistrySearch = useCallback((value: string) => {
    setRegistrySearchInput(value)
    setRegistrySearch(value)
    setRegistryPage(0)
  }, [])

  const applyWatchedSearch = useCallback((value: string) => {
    setWatchedSearchInput(value)
    setWatchedSearch(value)
    setWatchedPage(0)
  }, [])

  function clearCardErrors() {
    setError(null)
    setNotFound(false)
  }

  function clearDayErrors() {
    setDayError(null)
    setLimitExceeded(false)
  }

  async function loadWagonCardOnly(code: string, openedFromWatched: boolean) {
    const trimmed = code.trim()
    if (!trimmed) {
      setError('Укажите номер вагона')
      return
    }

    setCardLoading(true)
    clearCardErrors()
    setCard(null)
    setDay(null)
    clearDayErrors()
    setWagonCode(trimmed)
    setFromWatched(openedFromWatched)

    try {
      const wagonCard = await fetchWagonCard(trimmed)
      setCard(wagonCard)
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === 'WAGON_NOT_FOUND') {
          setNotFound(true)
          setError('Вагон не найден')
        } else {
          setError(err.message)
        }
      } else {
        setError(err instanceof Error ? err.message : 'Неизвестная ошибка')
      }
    } finally {
      setCardLoading(false)
    }
  }

  async function loadDayHistory() {
    const trimmed = wagonCode.trim()
    if (!trimmed || !date) {
      setDayError('Укажите номер вагона и дату')
      return
    }

    setDayLoading(true)
    clearDayErrors()
    setDay(null)

    try {
      const stateDay = await fetchStateReports(trimmed, date.format('YYYY-MM-DD'))
      setDay(stateDay)
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === 'WAGON_NOT_FOUND') {
          setNotFound(true)
          setDayError('Вагон не найден')
        } else if (err.code === 'RESULT_LIMIT_EXCEEDED') {
          setLimitExceeded(true)
          setDayError('Превышен лимит 500 строк за сутки. Результат не обрезан молча.')
        } else {
          setDayError(err.message)
        }
      } else {
        setDayError(err instanceof Error ? err.message : 'Неизвестная ошибка')
      }
    } finally {
      setDayLoading(false)
    }
  }

  // Restore card only when URL explicitly includes wagon= (F5/share). Day stays explicit.
  useEffect(() => {
    if (!hasWagonDeepLink(window.location.search)) {
      return
    }
    const link = readDeepLinkFromSearch(window.location.search)
    void loadWagonCardOnly(link.wagon, link.tab === 'watched')
    // Mount-only deep-link restore; loadWagonCardOnly is intentionally omitted.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const emptyDay = Boolean(day && day.lines.length === 0 && !dayError)
  const registryEmpty = !registryLoading && registryRows.length === 0 && !registryError
  const watchedEmpty = !watchedLoading && watchedRows.length === 0 && !watchedError
  const pageSizeOptions = LIST_PAGE_SIZE_OPTIONS.map(String)

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
            onChange={(key) => setActiveTab(parseListTab(key))}
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
                      <Col xs={24} md={8}>
                        <Input
                          size="large"
                          placeholder="Поиск по номеру (contains)"
                          value={watchedSearchInput}
                          onChange={(e) => {
                            const value = e.target.value
                            if (value === '') {
                              applyWatchedSearch('')
                            } else {
                              setWatchedSearchInput(value)
                            }
                          }}
                          onPressEnter={() => applyWatchedSearch(watchedSearchInput)}
                          allowClear
                        />
                      </Col>
                      <Col xs={24} md={5}>
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
                      <Col xs={24} md={5}>
                        <Button
                          type="primary"
                          size="large"
                          block
                          onClick={() => applyWatchedSearch(watchedSearchInput)}
                        >
                          Найти
                        </Button>
                      </Col>
                      <Col xs={24} md={6}>
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
                          onSelect={(code) => void loadWagonCardOnly(code, true)}
                        />
                      )}
                      <Flex justify="end" style={{ marginTop: 12 }}>
                        <Pagination
                          current={watchedPage + 1}
                          pageSize={watchedSize}
                          total={watchedTotal}
                          showSizeChanger
                          showQuickJumper
                          pageSizeOptions={pageSizeOptions}
                          showTotal={(total, range) =>
                            formatListShowTotal(total, range, watchedPage, watchedSize)
                          }
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
                      <Col xs={24} md={8}>
                        <Input
                          size="large"
                          placeholder="Поиск по номеру (contains)"
                          value={registrySearchInput}
                          onChange={(e) => {
                            const value = e.target.value
                            if (value === '') {
                              applyRegistrySearch('')
                            } else {
                              setRegistrySearchInput(value)
                            }
                          }}
                          onPressEnter={() => applyRegistrySearch(registrySearchInput)}
                          allowClear
                        />
                      </Col>
                      <Col xs={24} md={5}>
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
                      <Col xs={24} md={5}>
                        <Button
                          type="primary"
                          size="large"
                          block
                          onClick={() => applyRegistrySearch(registrySearchInput)}
                        >
                          Найти
                        </Button>
                      </Col>
                      <Col xs={24} md={6}>
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
                          onSelect={(code) => void loadWagonCardOnly(code, false)}
                        />
                      )}
                      <Flex justify="end" style={{ marginTop: 12 }}>
                        <Pagination
                          current={registryPage + 1}
                          pageSize={registrySize}
                          total={registryTotal}
                          showSizeChanger
                          showQuickJumper
                          pageSizeOptions={pageSizeOptions}
                          showTotal={(total, range) =>
                            formatListShowTotal(total, range, registryPage, registrySize)
                          }
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
            Карточка загружается отдельно; сутки — по явной кнопке (R1). Deep-link:{' '}
            <Text code>{buildDeepLinkSearch(deepLinkState)}</Text>
          </Paragraph>
          <Row gutter={[12, 12]} align="middle">
            <Col xs={24} md={8}>
              <Input
                size="large"
                placeholder="Номер вагона"
                value={wagonCode}
                onChange={(e) => setWagonCode(e.target.value)}
                onPressEnter={() => void loadWagonCardOnly(wagonCode, activeTab === 'watched')}
                allowClear
              />
            </Col>
            <Col xs={24} md={6}>
              <DatePicker
                size="large"
                style={{ width: '100%' }}
                value={date}
                onChange={(value) => {
                  if (value) {
                    setDate(value)
                    setDay(null)
                    clearDayErrors()
                  }
                }}
                format="YYYY-MM-DD"
              />
            </Col>
            <Col xs={24} md={5}>
              <Button
                type="primary"
                size="large"
                block
                loading={cardLoading}
                onClick={() => void loadWagonCardOnly(wagonCode, activeTab === 'watched')}
              >
                Найти
              </Button>
            </Col>
            <Col xs={24} md={5}>
              <Button
                size="large"
                block
                loading={dayLoading}
                onClick={() => void loadDayHistory()}
              >
                Загрузить сутки
              </Button>
            </Col>
          </Row>
        </Card>

        <Space orientation="vertical" size="large" style={{ width: '100%', marginTop: 24 }}>
          {(cardLoading || dayLoading) && (
            <Flex justify="center" style={{ padding: 32 }}>
              <Spin size="large" description={cardLoading ? 'Загрузка карточки…' : 'Загрузка суток…'} />
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
          {error && !notFound && <Alert type="error" showIcon title={error} />}
          {dayError && !notFound && !limitExceeded && (
            <Alert type="error" showIcon title={dayError} />
          )}

          {!cardLoading && card && (
            <Card>
              <WagonCardPanel card={card} fromWatched={fromWatched} />
            </Card>
          )}

          {!dayLoading && day && (
            <Card title={`История дислокации · ${day.date}`}>
              <StateReportGrid lines={day.lines} empty={emptyDay} />
            </Card>
          )}
        </Space>
      </Content>
    </Layout>
  )
}
