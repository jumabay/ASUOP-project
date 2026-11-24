/** Empty / unknown UI placeholder (P5-C). */
export const EMPTY_DISPLAY = '—'

export function displayValue(value: string | null | undefined): string {
  if (value == null || value.trim() === '') {
    return EMPTY_DISPLAY
  }
  return value
}

/** Boolean with explicit unknown (null) — never invent Да/Нет. */
export function displayBoolean(value: boolean | null | undefined): string {
  if (value == null) {
    return EMPTY_DISPLAY
  }
  return value ? 'Да' : 'Нет'
}
