import { AgGridReact } from 'ag-grid-react'
import type { ColDef, RowClickedEvent } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { Empty } from 'antd'
import { useMemo } from 'react'
import type { WagonRegistryItem } from '../types'
import { displayBoolean, displayValue } from '../utils/displayValue'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-quartz.css'

ModuleRegistry.registerModules([AllCommunityModule])

interface Props {
  rows: WagonRegistryItem[]
  empty: boolean
  onSelect: (wagonCode: string) => void
  /** When list JDBC is not wired (e.g. dbtc-ro). */
  emptyDescription?: string
}

/** Exported for Vitest contract checks (AG Grid headers are unreliable in jsdom). */
export const REGISTRY_COLUMN_FIELDS = [
  'wagonCode',
  'model',
  'wagonKind',
  'ownerName',
  'homeStationCode',
  'homeStationName',
  'inventoryPark',
] as const

export function buildRegistryColumnDefs(): ColDef<WagonRegistryItem>[] {
  return [
    { field: 'wagonCode', headerName: 'Номер вагона', minWidth: 140, flex: 1 },
    {
      field: 'model',
      headerName: 'Модель',
      minWidth: 100,
      valueFormatter: (p) => displayValue(p.value as string | null | undefined),
    },
    {
      field: 'wagonKind',
      headerName: 'Род',
      minWidth: 110,
      valueFormatter: (p) => displayValue(p.value as string | null | undefined),
    },
    {
      field: 'ownerName',
      headerName: 'Собственник',
      minWidth: 140,
      flex: 1,
      valueFormatter: (p) => displayValue(p.value as string | null | undefined),
    },
    {
      field: 'homeStationCode',
      headerName: 'Ст. код',
      minWidth: 90,
      valueFormatter: (p) => displayValue(p.value as string | null | undefined),
    },
    {
      field: 'homeStationName',
      headerName: 'Ст. приписки',
      minWidth: 140,
      flex: 1,
      valueFormatter: (p) => displayValue(p.value as string | null | undefined),
    },
    {
      field: 'inventoryPark',
      headerName: 'Инв. парк',
      minWidth: 100,
      valueFormatter: (p) => displayBoolean(p.value as boolean | null | undefined),
    },
  ]
}

export function WagonRegistryGrid({
  rows,
  empty,
  onSelect,
  emptyDescription = 'По заданным условиям вагоны не найдены',
}: Props) {
  const columnDefs = useMemo<ColDef<WagonRegistryItem>[]>(() => buildRegistryColumnDefs(), [])

  if (empty) {
    return (
      <div data-testid="registry-empty">
        <Empty description={emptyDescription} />
      </div>
    )
  }

  return (
    <div
      className="ag-theme-quartz state-report-grid"
      style={{ height: 520, width: '100%' }}
      data-testid="wagon-registry-grid"
    >
      <AgGridReact<WagonRegistryItem>
        rowData={rows}
        columnDefs={columnDefs}
        getRowId={(params) => params.data.wagonCode}
        suppressCellFocus
        animateRows={false}
        onRowClicked={(event: RowClickedEvent<WagonRegistryItem>) => {
          if (event.data?.wagonCode) {
            onSelect(event.data.wagonCode)
          }
        }}
      />
    </div>
  )
}
