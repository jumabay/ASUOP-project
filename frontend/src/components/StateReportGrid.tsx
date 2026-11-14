import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { Empty } from 'antd'
import { useMemo } from 'react'
import type { StateReportLine } from '../types'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-quartz.css'

ModuleRegistry.registerModules([AllCommunityModule])

interface Props {
  lines: StateReportLine[]
  empty: boolean
}

export function StateReportGrid({ lines, empty }: Props) {
  const columnDefs = useMemo<ColDef<StateReportLine>[]>(
    () => [
      { field: 'reportId', headerName: 'ID отчёта', width: 120 },
      { field: 'dislocationTime', headerName: 'Время дислокации', flex: 1, minWidth: 180 },
      { field: 'stationCode', headerName: 'Станция', width: 120 },
      { field: 'operationCode', headerName: 'Операция', width: 120 },
      { field: 'wagonCode', headerName: 'Вагон', width: 130 },
    ],
    [],
  )

  if (empty) {
    return <Empty description="За выбранные сутки записей нет" />
  }

  return (
    <div className="ag-theme-quartz state-report-grid" style={{ height: 360, width: '100%' }}>
      <AgGridReact<StateReportLine>
        rowData={lines}
        columnDefs={columnDefs}
        getRowId={(params) => String(params.data.reportId)}
        suppressCellFocus
        animateRows
      />
    </div>
  )
}
