export type ListTab = 'registry' | 'watched'

export interface DeepLinkState {
  tab: ListTab
  wagon: string
  date: string
}

const DEFAULT_TAB: ListTab = 'watched'
const DEFAULT_WAGON = 'SYN-00001'
const DEFAULT_DATE = '2024-01-15'

export function parseListTab(value: string | null | undefined): ListTab {
  return value === 'registry' ? 'registry' : DEFAULT_TAB
}

export function readDeepLinkFromSearch(search: string): DeepLinkState {
  const params = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search)
  const wagon = (params.get('wagon') ?? DEFAULT_WAGON).trim() || DEFAULT_WAGON
  const date = (params.get('date') ?? DEFAULT_DATE).trim() || DEFAULT_DATE
  return {
    tab: parseListTab(params.get('tab')),
    wagon,
    date,
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
  return `?${params.toString()}`
}

export function writeDeepLinkToUrl(state: DeepLinkState): void {
  if (typeof window === 'undefined') {
    return
  }
  const next = buildDeepLinkSearch(state)
  if (window.location.search === next) {
    return
  }
  window.history.replaceState(null, '', `${window.location.pathname}${next}`)
}
