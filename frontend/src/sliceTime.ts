import type { StateReportLine } from './types'

/**
 * Display clock for A1 «Время среза».
 * Prefer snapshotTime; fall back to dislocationTime for old backends.
 * Returns the API string as-is (no Date parse → no browser TZ shift).
 */
export function sliceTimeDisplay(line: Pick<StateReportLine, 'snapshotTime' | 'dislocationTime'>): string {
  const snapshot = line.snapshotTime
  if (snapshot != null && String(snapshot).trim() !== '') {
    return String(snapshot)
  }
  return line.dislocationTime ?? ''
}
