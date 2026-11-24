import { describe, expect, it } from 'vitest'
import { sliceTimeDisplay } from './sliceTime'

describe('sliceTimeDisplay', () => {
  it('prefers snapshotTime when present', () => {
    expect(
      sliceTimeDisplay({
        snapshotTime: '2024-01-15T08:00:00',
        dislocationTime: '2099-01-01T00:00:00',
      }),
    ).toBe('2024-01-15T08:00:00')
  })

  it('falls back to dislocationTime when snapshotTime missing', () => {
    expect(
      sliceTimeDisplay({
        dislocationTime: '2024-01-15T08:00:00',
      }),
    ).toBe('2024-01-15T08:00:00')
  })

  it('falls back when snapshotTime blank', () => {
    expect(
      sliceTimeDisplay({
        snapshotTime: '  ',
        dislocationTime: '2024-01-15T09:00:00',
      }),
    ).toBe('2024-01-15T09:00:00')
  })

  it('does not convert via Date (preserves fractional / naive ISO)', () => {
    const raw = '2024-01-15T23:59:59.5'
    expect(sliceTimeDisplay({ snapshotTime: raw, dislocationTime: raw })).toBe(raw)
  })
})
