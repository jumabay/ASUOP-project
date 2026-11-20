import { AgGridReact } from 'ag-grid-react'
import type { ColDef, RowClickedEvent } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { Empty } from 'antd'
import { useMemo } from 'react'
import type { WatchedWagonItem } from '../types'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-quartz.css'

ModuleRegistry.registerModules([AllCommunityModule])

interface Props {
  rows: WatchedWagonItem[]
  empty: boolean
  onSelect: (wagonCode: string) => void
}

export function WatchedWagonGrid({ rows, empty, onSelect }: Props) {
  const columnDefs = useMemo<ColDef<WatchedWagonItem>[]>(
    () => [
      { field: 'wagonCode', headerName: 'Номер вагона', minWidth: 140, flex: 1 },
      { field: 'model', headerName: 'Модель', minWidth: 100 },
      { field: 'wagonKind', headerName: 'Род', minWidth: 110 },
      { field: 'ownerName', headerName: 'Собственник', minWidth: 140, flex: 1 },
      { field: 'homeStationCode', headerName: 'Ст. код', minWidth: 90 },
      { field: 'homeStationName', headerName: 'Ст. приписки', minWidth: 140, flex: 1 },
      {
        field: 'inventoryPark',
        headerName: 'Инв. парк',
        minWidth: 100,
        valueFormatter: (p) => (p.value ? 'Да' : 'Нет'),
      },
    ],
    [],
  )

  if (empty) {
    return <Empty description="По заданным условиям вагоны не найдены" />
  }

  return (
    <div className="ag-theme-quartz state-report-grid" style={{ height: 560, width: '100%' }}>
      <AgGridReact<WatchedWagonItem>
        rowData={rows}
        columnDefs={columnDefs}
        getRowId={(params) => params.data.wagonCode}
        suppressCellFocus
        animateRows={false}
        onRowClicked={(event: RowClickedEvent<WatchedWagonItem>) => {
          if (event.data?.wagonCode) {
            onSelect(event.data.wagonCode)
          }
        }}
      />
    </div>
  )
}
