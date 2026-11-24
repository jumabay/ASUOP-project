import { WATCHED_LIST_MAX_SIZE } from './listPaging'

export type ListTab = 'registry' | 'watched'

export type ListSort = 'code,asc' | 'code,desc'

export interface DeepLinkState {
  tab: ListTab
  wagon: string
  date: string
  /** Active list contains-search; omitted from URL when empty. */
  search: string
  /** Owner contains filter; omitted from URL when empty. */
  owner: string
  /** Kind equals filter; omitted from URL when empty. */
  kind: string
  /** 0-based page index. */
  page: number
  size: number
  sort: ListSort
}

const DEFAULT_TAB: ListTab = 'watched'
const DEFAULT_WAGON = 'SYN-00001'
const DEFAULT_DATE = '2024-01-15'
const DEFAULT_SEARCH = ''
const DEFAULT_OWNER = ''
const DEFAULT_KIND = ''
const DEFAULT_PAGE = 0
const DEFAULT_SIZE = 50
const DEFAULT_SORT: ListSort = 'code,asc'

/** Server / UI max page size (R2/R3). */
export const DEEP_LINK_MAX_SIZE = 200

export const DEFAULT_DEEP_LINK: DeepLinkState = {
  tab: DEFAULT_TAB,
  wagon: DEFAULT_WAGON,
  date: DEFAULT_DATE,
  search: DEFAULT_SEARCH,
  owner: DEFAULT_OWNER,
  kind: DEFAULT_KIND,
  page: DEFAULT_PAGE,
  size: DEFAULT_SIZE,
  sort: DEFAULT_SORT,
}

export function parseListTab(value: string | null | undefined): ListTab {
  return value === 'registry' ? 'registry' : DEFAULT_TAB
}

export function parseListSort(value: string | null | undefined): ListSort {
  return value === 'code,desc' ? 'code,desc' : DEFAULT_SORT
}

function parseNonNegativeInt(value: string | null | undefined, fallback: number): number {
  if (value == null || value.trim() === '') {
    return fallback
  }
  const n = Number.parseInt(value, 10)
  if (!Number.isFinite(n) || n < 0) {
    return fallback
  }
  return n
}

function maxSizeForTab(tab: ListTab): number {
  return tab === 'watched' ? WATCHED_LIST_MAX_SIZE : DEEP_LINK_MAX_SIZE
}

function clampSize(size: number, tab: ListTab): number {
  if (size <= 0) {
    return DEFAULT_SIZE
  }
  return Math.min(size, maxSizeForTab(tab))
}

export function readDeepLinkFromSearch(search: string): DeepLinkState {
  const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search)
  const wagon = (params.get('wagon') ?? DEFAULT_WAGON).trim() || DEFAULT_WAGON
  const date = (params.get('date') ?? DEFAULT_DATE).trim() || DEFAULT_DATE
  const listSearch = (params.get('search') ?? DEFAULT_SEARCH).trim()
  const owner = (params.get('owner') ?? DEFAULT_OWNER).trim()
  const kind = (params.get('kind') ?? DEFAULT_KIND).trim()
  const tab = parseListTab(params.get('tab'))
  return {
    tab,
    wagon,
    date,
    search: listSearch,
    owner,
    kind,
    page: parseNonNegativeInt(params.get('page'), DEFAULT_PAGE),
    size: clampSize(parseNonNegativeInt(params.get('size'), DEFAULT_SIZE), tab),
    sort: parseListSort(params.get('sort')),
  }
}

/** True when share/F5 URL carries an explicit wagon= to restore the card. */
export function hasWagonDeepLink(search: string): boolean {
  const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search)
  return params.has('wagon')
}

export function buildDeepLinkSearch(state: DeepLinkState): string {
  const params = new URLSearchParams()
  params.set('tab', state.tab)
  params.set('wagon', state.wagon)
  params.set('date', state.date)
  const trimmedSearch = state.search.trim()
  if (trimmedSearch) {
    params.set('search', trimmedSearch)
  }
  const trimmedOwner = state.owner.trim()
  if (trimmedOwner) {
    params.set('owner', trimmedOwner)
  }
  const trimmedKind = state.kind.trim()
  if (trimmedKind) {
    params.set('kind', trimmedKind)
  }
  params.set('page', String(Math.max(0, state.page)))
  params.set('size', String(clampSize(state.size, state.tab)))
  params.set('sort', parseListSort(state.sort))
  return `?${params.toString()}`
}

export type DeepLinkWriteMode = 'replace' | 'push'

export function writeDeepLinkToUrl(
  state: DeepLinkState,
  mode: DeepLinkWriteMode = 'replace',
): void {
  if (typeof window === 'undefined') {
    return
  }
  const next = buildDeepLinkSearch(state)
  if (window.location.search === next) {
    return
  }
  const url = `${window.location.pathname}${next}`
  if (mode === 'push') {
    window.history.pushState(null, '', url)
  } else {
    window.history.replaceState(null, '', url)
  }
}
