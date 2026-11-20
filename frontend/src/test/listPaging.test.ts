import { describe, expect, it } from 'vitest'
import {
  formatListShowTotal,
  isLargeRegistryTotal,
  LIST_PAGE_SIZE_OPTIONS,
  LIST_SEARCH_DEBOUNCE_MS,
  totalPages,
} from '../utils/listPaging'

describe('listPaging UX math', () => {
  it('keeps debounce in the approved 250–400 ms window', () => {
    expect(LIST_SEARCH_DEBOUNCE_MS).toBeGreaterThanOrEqual(250)
    expect(LIST_SEARCH_DEBOUNCE_MS).toBeLessThanOrEqual(400)
  })

  it('does not offer page sizes above server MAX_SIZE 200', () => {
    expect(LIST_PAGE_SIZE_OPTIONS).toEqual([50, 100, 200])
    expect(Math.max(...LIST_PAGE_SIZE_OPTIONS)).toBe(200)
  })

  it('computes page count for 18k registry at size 50', () => {
    expect(totalPages(18_000, 50)).toBe(360)
    expect(totalPages(18_000, 200)).toBe(90)
    expect(totalPages(0, 50)).toBe(0)
  })

  it('formats showTotal with page X of Y and totalElements', () => {
    expect(formatListShowTotal(18_000, [1, 50], 0, 50)).toBe(
      '1–50 из 18000 · стр. 1 из 360',
    )
    expect(formatListShowTotal(18_000, [17951, 18000], 359, 50)).toBe(
      '17951–18000 из 18000 · стр. 360 из 360',
    )
    expect(isLargeRegistryTotal(18_000)).toBe(true)
    expect(isLargeRegistryTotal(250)).toBe(false)
  })
})
