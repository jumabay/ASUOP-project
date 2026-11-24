import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { Empty, Space } from 'antd'
import { useMemo } from 'react'
import type { StateReportLine } from '../types'
import { DAY_LINE_MAX, DayReportStatus } from './DayReportStatus'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-quartz.css'

ModuleRegistry.registerModules([AllCommunityModule])

interface Props {
  lines: StateReportLine[]
  empty: boolean
  loading?: boolean
  error?: string | null
  onRetry?: () => void
  /** When set (data loaded), shows caption N / max 500. */
  lineCount?: number | null
  maxLines?: number
}

export function StateReportGrid({
  lines,
  empty,
  loading = false,
  error = null,
  onRetry,
  lineCount = null,
  maxLines = DAY_LINE_MAX,
}: Props) {
  const columnDefs = useMemo<ColDef<StateReportLine>[]>(
    () => [
      { field: 'reportId', headerName: 'ID отчёта', width: 120, sortable: true },
      {
        field: 'dislocationTime',
        // P5-E: GETTING_DATE day axis — slice/kit time, not verified factual LOCATION_DATE.
        headerName: 'Время среза',
        flex: 1,
        minWidth: 180,
        sortable: true,
      },
      { field: 'stationCode', headerName: 'Станция', width: 120, sortable: true },
      { field: 'operationCode', headerName: 'Операция', width: 120, sortable: true },
      { field: 'wagonCode', headerName: 'Вагон', width: 130, sortable: true },
    ],
    [],
  )

  if (loading || error) {
    return (
      <DayReportStatus
        loading={loading}
        error={error}
        onRetry={onRetry}
        lineCount={null}
        maxLines={maxLines}
      />
    )
  }

  return (
    <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
      <DayReportStatus lineCount={lineCount} maxLines={maxLines} />
      {empty ? (
        <Empty description="За выбранные сутки записей нет" />
      ) : (
        <div className="ag-theme-quartz state-report-grid" style={{ height: 360, width: '100%' }}>
          <AgGridReact<StateReportLine>
            rowData={lines}
            columnDefs={columnDefs}
            getRowId={(params) => String(params.data.reportId)}
            suppressCellFocus
            animateRows={false}
          />
        </div>
      )}
    </Space>
  )
}

export { DAY_LINE_MAX }
