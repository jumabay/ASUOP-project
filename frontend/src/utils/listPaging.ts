/** Default debounce for registry / watched list search (P0: 250–400 ms). */
export const LIST_SEARCH_DEBOUNCE_MS = 300

/** Max page size offered in UI; must stay ≤ server MAX_SIZE (200). */
export const LIST_PAGE_SIZE_OPTIONS = [50, 100, 200] as const

export function totalPages(totalElements: number, pageSize: number): number {
  if (pageSize <= 0) {
    return 0
  }
  if (totalElements <= 0) {
    return 0
  }
  return Math.ceil(totalElements / pageSize)
}

/**
 * Ant Design Pagination showTotal text: range + page X of Y + totalElements.
 * `currentPage` is 0-based (API page index).
 */
export function formatListShowTotal(
  total: number,
  range: [number, number],
  currentPage: number,
  pageSize: number,
): string {
  const pages = Math.max(1, totalPages(total, pageSize))
  const pageNumber = total === 0 ? 0 : currentPage + 1
  return `${range[0]}–${range[1]} из ${total} · стр. ${pageNumber} из ${pages}`
}

/** Whether UI should treat a list as “large registry” scale (~18k). */
export function isLargeRegistryTotal(totalElements: number): boolean {
  return totalElements >= 10_000
}
