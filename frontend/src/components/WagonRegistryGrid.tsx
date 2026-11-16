import { AgGridReact } from 'ag-grid-react'
import type { ColDef, RowClickedEvent } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { Empty } from 'antd'
import { useMemo } from 'react'
import type { WagonRegistryItem } from '../types'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-quartz.css'

ModuleRegistry.registerModules([AllCommunityModule])

interface Props {
  rows: WagonRegistryItem[]
  empty: boolean
  onSelect: (wagonCode: string) => void
}

export function WagonRegistryGrid({ rows, empty, onSelect }: Props) {
  const columnDefs = useMemo<ColDef<WagonRegistryItem>[]>(
    () => [{ field: 'wagonCode', headerName: 'Номер вагона', flex: 1, minWidth: 180 }],
    [],
  )

  if (empty) {
    return <Empty description="По заданным условиям вагоны не найдены" />
  }

  return (
    <div className="ag-theme-quartz state-report-grid" style={{ height: 360, width: '100%' }}>
      <AgGridReact<WagonRegistryItem>
        rowData={rows}
        columnDefs={columnDefs}
        getRowId={(params) => params.data.wagonCode}
        suppressCellFocus
        animateRows
        onRowClicked={(event: RowClickedEvent<WagonRegistryItem>) => {
          if (event.data?.wagonCode) {
            onSelect(event.data.wagonCode)
          }
        }}
      />
    </div>
  )
}
