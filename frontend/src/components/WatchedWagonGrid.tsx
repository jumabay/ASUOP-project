import { AgGridReact } from 'ag-grid-react'
import type { ColDef, RowClickedEvent } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { Empty, Flex, Spin } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { fetchWatchedWagons } from '../api/wagonsApi'
import type { WatchedMembershipSource, WatchedWagonItem } from '../types'
import { WatchedMembershipBanner } from './WatchedMembershipBanner'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-quartz.css'

ModuleRegistry.registerModules([AllCommunityModule])

interface Props {
  rows: WatchedWagonItem[]
  empty: boolean
  onSelect: (wagonCode: string) => void
  /** Optional; when omitted the grid loads honesty metadata from the API (avoids App.tsx edits). */
  membershipSource?: WatchedMembershipSource
  membershipNote?: string
}

export function WatchedWagonGrid({
  rows,
  empty,
  onSelect,
  membershipSource: membershipSourceProp,
  membershipNote: membershipNoteProp,
}: Props) {
  const [source, setSource] = useState<WatchedMembershipSource | null>(
    membershipSourceProp ?? null,
  )
  const [note, setNote] = useState(membershipNoteProp ?? '')
  const [sourceLoading, setSourceLoading] = useState(membershipSourceProp == null)

  useEffect(() => {
    if (membershipSourceProp != null) {
      setSource(membershipSourceProp)
      setNote(membershipNoteProp ?? '')
      setSourceLoading(false)
      return
    }
    let cancelled = false
    setSourceLoading(true)
    void fetchWatchedWagons({ page: 0, size: 1 })
      .then((page) => {
        if (cancelled) {
          return
        }
        setSource(page.membershipSource)
        setNote(page.membershipNote ?? '')
      })
      .catch(() => {
        if (!cancelled) {
          setSource(null)
          setNote('')
        }
      })
      .finally(() => {
        if (!cancelled) {
          setSourceLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [membershipSourceProp, membershipNoteProp])

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

  const emptyDescription =
    source === 'EMPTY_PENDING_G31'
      ? 'Подтверждённый источник слежения отсутствует (G31 OPEN). Список пуст.'
      : 'По заданным условиям вагоны не найдены'

  return (
    <div data-testid="watched-wagon-grid">
      {sourceLoading && (
        <Flex justify="center" style={{ paddingBottom: 8 }}>
          <Spin size="small" />
        </Flex>
      )}
      {source && <WatchedMembershipBanner source={source} note={note} />}
      {empty ? (
        <div
          data-testid={
            source === 'EMPTY_PENDING_G31'
              ? 'watched-empty-pending-g31'
              : 'watched-empty-filter'
          }
        >
          <Empty description={emptyDescription} />
        </div>
      ) : (
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
      )}
    </div>
  )
}
