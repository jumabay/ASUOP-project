import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Flex,
  Input,
  Pagination,
  Row,
  Select,
  Space,
  Spin,
  Tabs,
  Typography,
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  fetchPlatformRuntime,
  fetchStateReports,
  fetchWagonCard,
  fetchWagonRegistry,
  fetchWatchedWagons,
} from './api/wagonsApi'
import { AppShell } from './components/AppShell'
import { DayReportStatus } from './components/DayReportStatus'
import { GlobalWagonSearch } from './components/GlobalWagonSearch'
import { ListErrorRetry } from './components/ListErrorRetry'
import { StateReportGrid } from './components/StateReportGrid'
import { WagonCardPanel } from './components/WagonCardPanel'
import { WagonRegistryGrid } from './components/WagonRegistryGrid'
import { WatchedWagonGrid } from './components/WatchedWagonGrid'
import {
  ApiError,
  type PlatformRuntime,
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
  type DeepLinkState,
  type DeepLinkWriteMode,
  type ListSort,
  type ListTab,
} from './utils/deepLink'
import {
  formatListShowTotal,
  LIST_PAGE_SIZE_OPTIONS,
  LIST_SEARCH_DEBOUNCE_MS,
} from './utils/listPaging'
import './App.css'

const { Paragraph, Text } = Typography

function initialDeepLink(): DeepLinkState {
  if (typeof window === 'undefined') {
    return readDeepLinkFromSearch('')
  }
  return readDeepLinkFromSearch(window.location.search)
}

function applyListParamsToTab(
  link: DeepLinkState,
  setters: {
    setSearchInput: (v: string) => void
    setSearch: (v: string) => void
    setOwnerInput: (v: string) => void
    setOwner: (v: string) => void
    setKindInput: (v: string) => void
    setKind: (v: string) => void
    setSort: (v: ListSort) => void
    setPage: (v: number) => void
    setSize: (v: number) => void
  },
) {
  setters.setSearchInput(link.search)
  setters.setSearch(link.search)
  setters.setOwnerInput(link.owner)
  setters.setOwner(link.owner)
  setters.setKindInput(link.kind)
  setters.setKind(link.kind)
  setters.setSort(link.sort)
  setters.setPage(link.page)
  setters.setSize(link.size)
}

export default function App() {
  const boot = useMemo(() => initialDeepLink(), [])
  const [activeTab, setActiveTab] = useState<ListTab>(() => boot.tab)
  const [wagonCode, setWagonCode] = useState(() => boot.wagon)
  const [date, setDate] = useState<Dayjs>(() => dayjs(boot.date))
  const [cardLoading, setCardLoading] = useState(false)
  const [dayLoading, setDayLoading] = useState(false)
  const [card, setCard] = useState<WagonCard | null>(null)
  const [day, setDay] = useState<StateReportDay | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [dayError, setDayError] = useState<string | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [limitExceeded, setLimitExceeded] = useState(false)
  const [fromWatched, setFromWatched] = useState(() => boot.tab === 'watched')

  const [registrySearchInput, setRegistrySearchInput] = useState(() =>
    boot.tab === 'registry' ? boot.search : '',
  )
  const [registrySearch, setRegistrySearch] = useState(() =>
    boot.tab === 'registry' ? boot.search : '',
  )
  const [registryOwnerInput, setRegistryOwnerInput] = useState(() =>
    boot.tab === 'registry' ? boot.owner : '',
  )
  const [registryOwner, setRegistryOwner] = useState(() =>
    boot.tab === 'registry' ? boot.owner : '',
  )
  const [registryKindInput, setRegistryKindInput] = useState(() =>
    boot.tab === 'registry' ? boot.kind : '',
  )
  const [registryKind, setRegistryKind] = useState(() =>
    boot.tab === 'registry' ? boot.kind : '',
  )
  const [registrySort, setRegistrySort] = useState<ListSort>(() =>
    boot.tab === 'registry' ? boot.sort : 'code,asc',
  )
  const [registryPage, setRegistryPage] = useState(() =>
    boot.tab === 'registry' ? boot.page : 0,
  )
  const [registrySize, setRegistrySize] = useState(() =>
    boot.tab === 'registry' ? boot.size : 50,
  )
  const [registryRows, setRegistryRows] = useState<WagonRegistryItem[]>([])
  const [registryTotal, setRegistryTotal] = useState(0)
  const [registryLoading, setRegistryLoading] = useState(false)
  const [registryError, setRegistryError] = useState<string | null>(null)

  const [watchedSearchInput, setWatchedSearchInput] = useState(() =>
    boot.tab === 'watched' ? boot.search : '',
  )
  const [watchedSearch, setWatchedSearch] = useState(() =>
    boot.tab === 'watched' ? boot.search : '',
  )
  const [watchedOwnerInput, setWatchedOwnerInput] = useState(() =>
    boot.tab === 'watched' ? boot.owner : '',
  )
  const [watchedOwner, setWatchedOwner] = useState(() =>
    boot.tab === 'watched' ? boot.owner : '',
  )
  const [watchedKindInput, setWatchedKindInput] = useState(() =>
    boot.tab === 'watched' ? boot.kind : '',
  )
  const [watchedKind, setWatchedKind] = useState(() =>
    boot.tab === 'watched' ? boot.kind : '',
  )
  const [watchedSort, setWatchedSort] = useState<ListSort>(() =>
    boot.tab === 'watched' ? boot.sort : 'code,asc',
  )
  const [watchedPage, setWatchedPage] = useState(() =>
    boot.tab === 'watched' ? boot.page : 0,
  )
  const [watchedSize, setWatchedSize] = useState(() =>
    boot.tab === 'watched' ? boot.size : 50,
  )
  const [watchedRows, setWatchedRows] = useState<WatchedWagonItem[]>([])
  const [watchedTotal, setWatchedTotal] = useState(0)
  const [watchedLoading, setWatchedLoading] = useState(false)
  const [watchedError, setWatchedError] = useState<string | null>(null)
  const [platformRuntime, setPlatformRuntime] = useState<PlatformRuntime | null>(null)

  const urlWriteModeRef = useRef<DeepLinkWriteMode>('replace')
  const syncingFromUrlRef = useRef(false)

  useEffect(() => {
    let cancelled = false
    void fetchPlatformRuntime()
      .then((runtime) => {
        if (!cancelled) {
          setPlatformRuntime(runtime)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPlatformRuntime(null)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  const activeListParams = useMemo(() => {
    if (activeTab === 'registry') {
      return {
        search: registrySearch,
        owner: registryOwner,
        kind: registryKind,
        page: registryPage,
        size: registrySize,
        sort: registrySort,
      }
    }
    return {
      search: watchedSearch,
      owner: watchedOwner,
      kind: watchedKind,
      page: watchedPage,
      size: watchedSize,
      sort: watchedSort,
    }
  }, [
    activeTab,
    registrySearch,
    registryOwner,
    registryKind,
    registryPage,
    registrySize,
    registrySort,
    watchedSearch,
    watchedOwner,
    watchedKind,
    watchedPage,
    watchedSize,
    watchedSort,
  ])

  const deepLinkState: DeepLinkState = useMemo(
    () => ({
      tab: activeTab,
      wagon: wagonCode.trim() || 'SYN-00001',
      date: date.format('YYYY-MM-DD'),
      search: activeListParams.search,
      owner: activeListParams.owner,
      kind: activeListParams.kind,
      page: activeListParams.page,
      size: activeListParams.size,
      sort: activeListParams.sort,
    }),
    [activeTab, wagonCode, date, activeListParams],
  )

  const scheduleUrlPush = useCallback(() => {
    urlWriteModeRef.current = 'push'
  }, [])

  useEffect(() => {
    if (syncingFromUrlRef.current) {
      syncingFromUrlRef.current = false
      urlWriteModeRef.current = 'replace'
      return
    }
    const mode = urlWriteModeRef.current
    urlWriteModeRef.current = 'replace'
    writeDeepLinkToUrl(deepLinkState, mode)
  }, [deepLinkState])

  const restoreFromDeepLink = useCallback((link: DeepLinkState) => {
    syncingFromUrlRef.current = true
    setActiveTab(link.tab)
    setWagonCode(link.wagon)
    setDate(dayjs(link.date))
    setFromWatched(link.tab === 'watched')
    if (link.tab === 'registry') {
      applyListParamsToTab(link, {
        setSearchInput: setRegistrySearchInput,
        setSearch: setRegistrySearch,
        setOwnerInput: setRegistryOwnerInput,
        setOwner: setRegistryOwner,
        setKindInput: setRegistryKindInput,
        setKind: setRegistryKind,
        setSort: setRegistrySort,
        setPage: setRegistryPage,
        setSize: setRegistrySize,
      })
    } else {
      applyListParamsToTab(link, {
        setSearchInput: setWatchedSearchInput,
        setSearch: setWatchedSearch,
        setOwnerInput: setWatchedOwnerInput,
        setOwner: setWatchedOwner,
        setKindInput: setWatchedKindInput,
        setKind: setWatchedKind,
        setSort: setWatchedSort,
        setPage: setWatchedPage,
        setSize: setWatchedSize,
      })
    }
  }, [])

  useEffect(() => {
    const onPopState = () => {
      const link = readDeepLinkFromSearch(window.location.search)
      restoreFromDeepLink(link)
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [restoreFromDeepLink])

  useEffect(() => {
    if (registrySearchInput === registrySearch) {
      return
    }
    const timer = window.setTimeout(() => {
      setRegistrySearch(registrySearchInput)
      setRegistryPage(0)
      scheduleUrlPush()
    }, LIST_SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [registrySearchInput, registrySearch, scheduleUrlPush])

  useEffect(() => {
    if (registryOwnerInput === registryOwner) {
      return
    }
    const timer = window.setTimeout(() => {
      setRegistryOwner(registryOwnerInput)
      setRegistryPage(0)
      scheduleUrlPush()
    }, LIST_SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [registryOwnerInput, registryOwner, scheduleUrlPush])

  useEffect(() => {
    if (registryKindInput === registryKind) {
      return
    }
    const timer = window.setTimeout(() => {
      setRegistryKind(registryKindInput)
      setRegistryPage(0)
      scheduleUrlPush()
    }, LIST_SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [registryKindInput, registryKind, scheduleUrlPush])

  useEffect(() => {
    if (watchedSearchInput === watchedSearch) {
      return
    }
    const timer = window.setTimeout(() => {
      setWatchedSearch(watchedSearchInput)
      setWatchedPage(0)
      scheduleUrlPush()
    }, LIST_SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [watchedSearchInput, watchedSearch, scheduleUrlPush])

  useEffect(() => {
    if (watchedOwnerInput === watchedOwner) {
      return
    }
    const timer = window.setTimeout(() => {
      setWatchedOwner(watchedOwnerInput)
      setWatchedPage(0)
      scheduleUrlPush()
    }, LIST_SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [watchedOwnerInput, watchedOwner, scheduleUrlPush])

  useEffect(() => {
    if (watchedKindInput === watchedKind) {
      return
    }
    const timer = window.setTimeout(() => {
      setWatchedKind(watchedKindInput)
      setWatchedPage(0)
      scheduleUrlPush()
    }, LIST_SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [watchedKindInput, watchedKind, scheduleUrlPush])

  const loadRegistry = useCallback(async () => {
    setRegistryLoading(true)
    setRegistryError(null)
    try {
      const page = await fetchWagonRegistry({
        page: registryPage,
        size: registrySize,
        sort: registrySort,
        search: registrySearch,
        owner: registryOwner,
        kind: registryKind,
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
  }, [registryPage, registrySize, registrySort, registrySearch, registryOwner, registryKind])

  const loadWatched = useCallback(async () => {
    setWatchedLoading(true)
    setWatchedError(null)
    try {
      const page = await fetchWatchedWagons({
        page: watchedPage,
        size: watchedSize,
        sort: watchedSort,
        search: watchedSearch,
        owner: watchedOwner,
        kind: watchedKind,
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
  }, [watchedPage, watchedSize, watchedSort, watchedSearch, watchedOwner, watchedKind])

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

  const applyRegistryFilters = useCallback(
    (next: { search?: string; owner?: string; kind?: string }) => {
      if (next.search !== undefined) {
        setRegistrySearchInput(next.search)
        setRegistrySearch(next.search)
      }
      if (next.owner !== undefined) {
        setRegistryOwnerInput(next.owner)
        setRegistryOwner(next.owner)
      }
      if (next.kind !== undefined) {
        setRegistryKindInput(next.kind)
        setRegistryKind(next.kind)
      }
      setRegistryPage(0)
      scheduleUrlPush()
    },
    [scheduleUrlPush],
  )

  const applyWatchedFilters = useCallback(
    (next: { search?: string; owner?: string; kind?: string }) => {
      if (next.search !== undefined) {
        setWatchedSearchInput(next.search)
        setWatchedSearch(next.search)
      }
      if (next.owner !== undefined) {
        setWatchedOwnerInput(next.owner)
        setWatchedOwner(next.owner)
      }
      if (next.kind !== undefined) {
        setWatchedKindInput(next.kind)
        setWatchedKind(next.kind)
      }
      setWatchedPage(0)
      scheduleUrlPush()
    },
    [scheduleUrlPush],
  )

  const selectFleetTab = useCallback(
    (tab: ListTab) => {
      scheduleUrlPush()
      setActiveTab(tab)
    },
    [scheduleUrlPush],
  )

  function clearCardErrors() {
    setError(null)
    setNotFound(false)
  }

  function clearDayErrors() {
    setDayError(null)
    setLimitExceeded(false)
  }

  async function loadWagonCardOnly(
    code: string,
    openedFromWatched: boolean,
    options?: { pushUrl?: boolean },
  ) {
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
    if (options?.pushUrl !== false) {
      scheduleUrlPush()
    }

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
    void loadWagonCardOnly(link.wagon, link.tab === 'watched', { pushUrl: false })
    // Mount-only deep-link restore; loadWagonCardOnly is intentionally omitted.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const emptyDay = Boolean(day && day.lines.length === 0 && !dayError)
  const registryEmpty = !registryLoading && registryRows.length === 0 && !registryError
  const watchedEmpty = !watchedLoading && watchedRows.length === 0 && !watchedError
  const pageSizeOptions = LIST_PAGE_SIZE_OPTIONS.map(String)

  return (
    <AppShell
      activeTab={activeTab}
      onSelectFleetTab={selectFleetTab}
      dataSourceLabel={platformRuntime?.displayLabel}
      headerExtra={
        <GlobalWagonSearch
          initialCode={wagonCode}
          loading={cardLoading}
          onSearch={(code) => void loadWagonCardOnly(code, activeTab === 'watched')}
        />
      }
    >
      <Card className="search-card">
        <Tabs
          activeKey={activeTab}
          onChange={(key) => selectFleetTab(parseListTab(key))}
          items={[
            {
              key: 'watched',
              label: 'Вагоны на слежении',
              children: (
                <>
                  <Paragraph type="secondary" style={{ marginBottom: 16 }}>
                    {platformRuntime && !platformRuntime.watchedConnected
                      ? 'Источник не подключён. Пустой список не означает отсутствие вагонов. Карточка A1/A2 загружается отдельно.'
                      : 'Synthetic-подмножество каталога (60 вагонов). Паспорт A2, серверная пагинация. Клик открывает карточку R1. Соответствие GetActiveWagonList не подтверждено.'}
                  </Paragraph>
                  <Row gutter={[12, 12]} align="middle">
                    <Col xs={24} md={6}>
                      <Input
                        size="large"
                        placeholder="Поиск по номеру (contains)"
                        value={watchedSearchInput}
                        onChange={(e) => {
                          const value = e.target.value
                          if (value === '') {
                            applyWatchedFilters({ search: '' })
                          } else {
                            setWatchedSearchInput(value)
                          }
                        }}
                        onPressEnter={() =>
                          applyWatchedFilters({
                            search: watchedSearchInput,
                            owner: watchedOwnerInput,
                            kind: watchedKindInput,
                          })
                        }
                        allowClear
                      />
                    </Col>
                    <Col xs={24} md={5}>
                      <Input
                        size="large"
                        placeholder="Собственник (contains)"
                        value={watchedOwnerInput}
                        onChange={(e) => {
                          const value = e.target.value
                          if (value === '') {
                            applyWatchedFilters({ owner: '' })
                          } else {
                            setWatchedOwnerInput(value)
                          }
                        }}
                        onPressEnter={() =>
                          applyWatchedFilters({
                            search: watchedSearchInput,
                            owner: watchedOwnerInput,
                            kind: watchedKindInput,
                          })
                        }
                        allowClear
                      />
                    </Col>
                    <Col xs={24} md={5}>
                      <Input
                        size="large"
                        placeholder="Род (equals)"
                        value={watchedKindInput}
                        onChange={(e) => {
                          const value = e.target.value
                          if (value === '') {
                            applyWatchedFilters({ kind: '' })
                          } else {
                            setWatchedKindInput(value)
                          }
                        }}
                        onPressEnter={() =>
                          applyWatchedFilters({
                            search: watchedSearchInput,
                            owner: watchedOwnerInput,
                            kind: watchedKindInput,
                          })
                        }
                        allowClear
                      />
                    </Col>
                    <Col xs={24} md={4}>
                      <Select
                        size="large"
                        style={{ width: '100%' }}
                        value={watchedSort}
                        onChange={(value: ListSort) => {
                          scheduleUrlPush()
                          setWatchedPage(0)
                          setWatchedSort(value)
                        }}
                        options={[
                          { value: 'code,asc', label: 'Код ↑' },
                          { value: 'code,desc', label: 'Код ↓' },
                        ]}
                      />
                    </Col>
                    <Col xs={12} md={2}>
                      <Button
                        type="primary"
                        size="large"
                        block
                        onClick={() =>
                          applyWatchedFilters({
                            search: watchedSearchInput,
                            owner: watchedOwnerInput,
                            kind: watchedKindInput,
                          })
                        }
                      >
                        Найти
                      </Button>
                    </Col>
                    <Col xs={12} md={2}>
                      <Button
                        size="large"
                        block
                        loading={watchedLoading}
                        onClick={() => void loadWatched()}
                      >
                        Обновить
                      </Button>
                    </Col>
                  </Row>
                  <div style={{ marginTop: 16 }}>
                    {watchedLoading && (
                      <Flex justify="center" style={{ padding: 24 }}>
                        <Spin size="large" description="Загрузка слежения…" />
                      </Flex>
                    )}
                    {watchedError && (
                      <ListErrorRetry message={watchedError} onRetry={() => void loadWatched()} />
                    )}
                    {!watchedLoading && (
                      <WatchedWagonGrid
                        rows={watchedRows}
                        empty={watchedEmpty}
                        onSelect={(code) => void loadWagonCardOnly(code, true)}
                        membershipSource={
                          platformRuntime && !platformRuntime.watchedConnected
                            ? 'SOURCE_NOT_CONNECTED'
                            : undefined
                        }
                        membershipNote={
                          platformRuntime && !platformRuntime.watchedConnected
                            ? 'Источник не подключён. JDBC для списков registry/watched не утверждён. Пустой список не означает отсутствие вагонов.'
                            : undefined
                        }
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
                          scheduleUrlPush()
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
                    {platformRuntime && !platformRuntime.registryConnected
                      ? 'Источник не подключён. Пустой список не означает отсутствие вагонов. Карточка A1/A2 загружается отдельно.'
                      : 'Серверная пагинация (до 200), фильтры search/owner/kind (AND), сортировка по коду. Клик по строке открывает карточку R1.'}
                  </Paragraph>
                  <Row gutter={[12, 12]} align="middle">
                    <Col xs={24} md={6}>
                      <Input
                        size="large"
                        placeholder="Поиск по номеру (contains)"
                        value={registrySearchInput}
                        onChange={(e) => {
                          const value = e.target.value
                          if (value === '') {
                            applyRegistryFilters({ search: '' })
                          } else {
                            setRegistrySearchInput(value)
                          }
                        }}
                        onPressEnter={() =>
                          applyRegistryFilters({
                            search: registrySearchInput,
                            owner: registryOwnerInput,
                            kind: registryKindInput,
                          })
                        }
                        allowClear
                      />
                    </Col>
                    <Col xs={24} md={5}>
                      <Input
                        size="large"
                        placeholder="Собственник (contains)"
                        value={registryOwnerInput}
                        onChange={(e) => {
                          const value = e.target.value
                          if (value === '') {
                            applyRegistryFilters({ owner: '' })
                          } else {
                            setRegistryOwnerInput(value)
                          }
                        }}
                        onPressEnter={() =>
                          applyRegistryFilters({
                            search: registrySearchInput,
                            owner: registryOwnerInput,
                            kind: registryKindInput,
                          })
                        }
                        allowClear
                      />
                    </Col>
                    <Col xs={24} md={5}>
                      <Input
                        size="large"
                        placeholder="Род (equals)"
                        value={registryKindInput}
                        onChange={(e) => {
                          const value = e.target.value
                          if (value === '') {
                            applyRegistryFilters({ kind: '' })
                          } else {
                            setRegistryKindInput(value)
                          }
                        }}
                        onPressEnter={() =>
                          applyRegistryFilters({
                            search: registrySearchInput,
                            owner: registryOwnerInput,
                            kind: registryKindInput,
                          })
                        }
                        allowClear
                      />
                    </Col>
                    <Col xs={24} md={4}>
                      <Select
                        size="large"
                        style={{ width: '100%' }}
                        value={registrySort}
                        onChange={(value: ListSort) => {
                          scheduleUrlPush()
                          setRegistryPage(0)
                          setRegistrySort(value)
                        }}
                        options={[
                          { value: 'code,asc', label: 'Код ↑' },
                          { value: 'code,desc', label: 'Код ↓' },
                        ]}
                      />
                    </Col>
                    <Col xs={12} md={2}>
                      <Button
                        type="primary"
                        size="large"
                        block
                        onClick={() =>
                          applyRegistryFilters({
                            search: registrySearchInput,
                            owner: registryOwnerInput,
                            kind: registryKindInput,
                          })
                        }
                      >
                        Найти
                      </Button>
                    </Col>
                    <Col xs={12} md={2}>
                      <Button
                        size="large"
                        block
                        loading={registryLoading}
                        onClick={() => void loadRegistry()}
                      >
                        Обновить
                      </Button>
                    </Col>
                  </Row>
                  <div style={{ marginTop: 16 }}>
                    {registryLoading && (
                      <Flex justify="center" style={{ padding: 24 }}>
                        <Spin size="large" description="Загрузка реестра…" />
                      </Flex>
                    )}
                    {registryError && (
                      <ListErrorRetry
                        message={registryError}
                        onRetry={() => void loadRegistry()}
                      />
                    )}
                    {!registryLoading && (
                      <WagonRegistryGrid
                        rows={registryRows}
                        empty={registryEmpty}
                        emptyDescription={
                          platformRuntime && !platformRuntime.registryConnected
                            ? 'Источник не подключён'
                            : 'По заданным условиям вагоны не найдены'
                        }
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
                          scheduleUrlPush()
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
          <Text code data-testid="deep-link-preview">
            {buildDeepLinkSearch(deepLinkState)}
          </Text>
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
        {cardLoading && (
          <Flex justify="center" style={{ padding: 32 }}>
            <Spin size="large" description="Загрузка карточки…" />
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

        {!cardLoading && card && (
          <Card>
            <WagonCardPanel
              card={card}
              fromWatched={fromWatched}
              dataSourceKind={platformRuntime?.dataSourceKind}
            />
          </Card>
        )}

        {(dayLoading || (dayError && !notFound && !limitExceeded)) && (
          <DayReportStatus
            loading={dayLoading}
            error={dayError && !notFound && !limitExceeded ? dayError : null}
            onRetry={() => void loadDayHistory()}
          />
        )}

        {!dayLoading && day && (
          <Card title={`История дислокации · ${day.date}`}>
            <StateReportGrid
              lines={day.lines}
              empty={emptyDay}
              lineCount={day.lines.length}
              onRetry={() => void loadDayHistory()}
            />
          </Card>
        )}
      </Space>
    </AppShell>
  )
}
