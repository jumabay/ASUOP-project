export interface WagonCurrentState {
  stationCode: string | null
  operationCode: string | null
  dislocationTime: string | null
  trainIndex: string | null
}

export interface WagonCard {
  wagonCode: string
  model: string
  wagonKind: string
  ownerName: string
  homeStationCode: string
  homeStationName: string
  inventoryPark: boolean
  /** Optional P5-C block; null when synthetic current fixture is absent. */
  currentState?: WagonCurrentState | null
}

export interface WagonRegistryItem {
  wagonCode: string
  model: string
  wagonKind: string
  ownerName: string
  homeStationCode: string
  homeStationName: string
  inventoryPark: boolean
}

export interface WagonRegistryPage {
  content: WagonRegistryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface WatchedWagonItem {
  wagonCode: string
  model: string
  wagonKind: string
  ownerName: string
  homeStationCode: string
  homeStationName: string
  inventoryPark: boolean
}

/** Additive P5-D honesty label; FIXTURE ≠ company Active / G31. */
export type WatchedMembershipSource = 'FIXTURE' | 'EMPTY_PENDING_G31'

export interface WatchedWagonPage {
  content: WatchedWagonItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  /** Additive (P5-D); synthetic returns FIXTURE, RAIL returns EMPTY_PENDING_G31. */
  membershipSource: WatchedMembershipSource
  membershipNote: string
}

export interface StateReportLine {
  reportId: number
  wagonCode: string
  dislocationTime: string
  stationCode: string
  operationCode: string
}

export interface StateReportDay {
  wagonCode: string
  date: string
  lines: StateReportLine[]
}

export interface ApiErrorBody {
  error: string
  message: string
  wagonCode?: string | null
  date?: string | null
}

export class ApiError extends Error {
  readonly code: string
  readonly status: number

  constructor(status: number, body: ApiErrorBody) {
    super(body.message || body.error)
    this.name = 'ApiError'
    this.status = status
    this.code = body.error
  }
}
