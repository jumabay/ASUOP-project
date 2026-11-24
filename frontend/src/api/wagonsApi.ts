import type {
  PlatformRuntime,
  StateReportDay,
  WagonCard,
  WagonRegistryPage,
  WatchedWagonPage,
} from '../types'
import { ApiError, type ApiErrorBody } from '../types'

async function parseError(response: Response): Promise<never> {
  let body: ApiErrorBody
  try {
    body = (await response.json()) as ApiErrorBody
  } catch {
    body = { error: 'UNKNOWN', message: response.statusText || 'Request failed' }
  }
  throw new ApiError(response.status, body)
}

function appendListQuery(
  query: URLSearchParams,
  params: {
    page?: number
    size?: number
    sort?: 'code,asc' | 'code,desc'
    search?: string
    owner?: string
    kind?: string
  },
): void {
  if (params.page !== undefined) {
    query.set('page', String(params.page))
  }
  if (params.size !== undefined) {
    query.set('size', String(params.size))
  }
  if (params.sort) {
    query.set('sort', params.sort)
  }
  if (params.search !== undefined && params.search.trim() !== '') {
    query.set('search', params.search.trim())
  }
  if (params.owner !== undefined && params.owner.trim() !== '') {
    query.set('owner', params.owner.trim())
  }
  if (params.kind !== undefined && params.kind.trim() !== '') {
    query.set('kind', params.kind.trim())
  }
}

export async function fetchWagonRegistry(params: {
  page?: number
  size?: number
  sort?: 'code,asc' | 'code,desc'
  search?: string
  owner?: string
  kind?: string
}): Promise<WagonRegistryPage> {
  const query = new URLSearchParams()
  appendListQuery(query, params)
  const qs = query.toString()
  const response = await fetch(`/api/v1/wagons${qs ? `?${qs}` : ''}`)
  if (!response.ok) {
    await parseError(response)
  }
  return (await response.json()) as WagonRegistryPage
}

export async function fetchWatchedWagons(params: {
  page?: number
  size?: number
  sort?: 'code,asc' | 'code,desc'
  search?: string
  owner?: string
  kind?: string
}): Promise<WatchedWagonPage> {
  const query = new URLSearchParams()
  appendListQuery(query, params)
  const qs = query.toString()
  const response = await fetch(`/api/v1/wagons/watched${qs ? `?${qs}` : ''}`)
  if (!response.ok) {
    await parseError(response)
  }
  return (await response.json()) as WatchedWagonPage
}

export async function fetchWagonCard(wagonCode: string): Promise<WagonCard> {
  const response = await fetch(`/api/v1/wagons/${encodeURIComponent(wagonCode)}`)
  if (!response.ok) {
    await parseError(response)
  }
  return (await response.json()) as WagonCard
}

export async function fetchStateReports(
  wagonCode: string,
  date: string,
): Promise<StateReportDay> {
  const params = new URLSearchParams({ date })
  const response = await fetch(
    `/api/v1/wagons/${encodeURIComponent(wagonCode)}/state-reports?${params}`,
  )
  if (!response.ok) {
    await parseError(response)
  }
  return (await response.json()) as StateReportDay
}

/** Safe honesty endpoint — no JDBC URL / credentials. */
export async function fetchPlatformRuntime(): Promise<PlatformRuntime> {
  const response = await fetch('/api/v1/platform/runtime')
  if (!response.ok) {
    await parseError(response)
  }
  return (await response.json()) as PlatformRuntime
}
