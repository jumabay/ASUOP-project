import { render, screen } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import { describe, expect, it } from 'vitest'
import { WagonCardPanel } from '../components/WagonCardPanel'
import {
  REGISTRY_COLUMN_FIELDS,
  WagonRegistryGrid,
  buildRegistryColumnDefs,
} from '../components/WagonRegistryGrid'
import { displayValue, EMPTY_DISPLAY } from '../utils/displayValue'

describe('P5-C displayValue', () => {
  it('shows em dash for empty unknown values', () => {
    expect(displayValue(null)).toBe(EMPTY_DISPLAY)
    expect(displayValue(undefined)).toBe(EMPTY_DISPLAY)
    expect(displayValue('')).toBe(EMPTY_DISPLAY)
    expect(displayValue('  ')).toBe(EMPTY_DISPLAY)
    expect(displayValue('7100')).toBe('7100')
  })
})

describe('P5-C WagonRegistryGrid columns', () => {
  it('defines enriched passport columns', () => {
    const defs = buildRegistryColumnDefs()
    expect(defs.map((d) => d.field)).toEqual([...REGISTRY_COLUMN_FIELDS])
    expect(defs.map((d) => d.headerName)).toEqual([
      'Номер вагона',
      'Модель',
      'Род',
      'Собственник',
      'Ст. код',
      'Ст. приписки',
      'Инв. парк',
    ])
  })

  it('mounts registry grid with test id', () => {
    render(
      <ConfigProvider>
        <WagonRegistryGrid
          empty={false}
          onSelect={() => undefined}
          rows={[
            {
              wagonCode: 'SYN-00001',
              model: '12-132',
              wagonKind: 'КРЫТЫЙ',
              ownerName: 'SYN-OWNER',
              homeStationCode: '6900',
              homeStationName: 'SYN-STATION',
              inventoryPark: true,
            },
          ]}
        />
      </ConfigProvider>,
    )
    expect(screen.getByTestId('wagon-registry-grid')).toBeInTheDocument()
  })
})

describe('P5-C WagonCardPanel current state', () => {
  it('shows current block values when present', () => {
    render(
      <ConfigProvider>
        <WagonCardPanel
          card={{
            wagonCode: 'SYN-00001',
            model: '12-132',
            wagonKind: 'КРЫТЫЙ',
            ownerName: 'SYN-OWNER',
            homeStationCode: '6900',
            homeStationName: 'SYN-STATION',
            inventoryPark: true,
            currentState: {
              stationCode: '7100',
              operationCode: '05',
              dislocationTime: '2024-01-16T06:00:00',
              trainIndex: 'TRN-DIFF',
            },
          }}
        />
      </ConfigProvider>,
    )
    const block = screen.getByTestId('wagon-card-current-state')
    expect(block).toHaveTextContent('Текущее состояние')
    expect(block).toHaveTextContent('7100')
    expect(block).toHaveTextContent('05')
    expect(block).toHaveTextContent('2024-01-16T06:00:00')
    expect(block).toHaveTextContent('TRN-DIFF')
  })

  it('shows em dash when currentState is null', () => {
    render(
      <ConfigProvider>
        <WagonCardPanel
          card={{
            wagonCode: 'SYN-00002',
            model: '12-132',
            wagonKind: 'КРЫТЫЙ',
            ownerName: 'SYN-OWNER',
            homeStationCode: '6900',
            homeStationName: 'SYN-STATION',
            inventoryPark: true,
            currentState: null,
          }}
        />
      </ConfigProvider>,
    )
    const block = screen.getByTestId('wagon-card-current-state')
    expect(block).toHaveTextContent(EMPTY_DISPLAY)
  })

  it('shows em dash for blank current fields', () => {
    render(
      <ConfigProvider>
        <WagonCardPanel
          card={{
            wagonCode: 'SYN-00003',
            model: '12-132',
            wagonKind: 'КРЫТЫЙ',
            ownerName: 'SYN-OWNER',
            homeStationCode: '6900',
            homeStationName: 'SYN-STATION',
            inventoryPark: true,
            currentState: {
              stationCode: null,
              operationCode: '',
              dislocationTime: '  ',
              trainIndex: null,
            },
          }}
        />
      </ConfigProvider>,
    )
    const block = screen.getByTestId('wagon-card-current-state')
    expect(block.textContent?.includes(EMPTY_DISPLAY)).toBe(true)
  })
})
