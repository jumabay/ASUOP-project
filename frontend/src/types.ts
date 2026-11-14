export interface WagonCard {
  wagonCode: string
  model: string
  wagonKind: string
  ownerName: string
  homeStationCode: string
  homeStationName: string
  inventoryPark: boolean
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
