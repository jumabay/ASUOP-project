import type { StateReportDay, WagonCard, WagonRegistryPage } from '../types'
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

export async function fetchWagonRegistry(params: {
  page?: number
  size?: number
  sort?: 'code,asc' | 'code,desc'
  search?: string
}): Promise<WagonRegistryPage> {
  const query = new URLSearchParams()
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
  const qs = query.toString()
  const response = await fetch(`/api/v1/wagons${qs ? `?${qs}` : ''}`)
  if (!response.ok) {
    await parseError(response)
  }
  return (await response.json()) as WagonRegistryPage
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
