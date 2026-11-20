import { describe, expect, it } from 'vitest'
import {
  buildDeepLinkSearch,
  hasWagonDeepLink,
  parseListTab,
  readDeepLinkFromSearch,
} from '../utils/deepLink'

describe('deepLink query string', () => {
  it('parses tab, wagon, date from search', () => {
    expect(readDeepLinkFromSearch('?tab=registry&wagon=SYN-00042&date=2024-01-16')).toEqual({
      tab: 'registry',
      wagon: 'SYN-00042',
      date: '2024-01-16',
    })
  })

  it('defaults missing params for F5-safe restore', () => {
    expect(readDeepLinkFromSearch('')).toEqual({
      tab: 'watched',
      wagon: 'SYN-00001',
      date: '2024-01-15',
    })
    expect(parseListTab('watched')).toBe('watched')
    expect(parseListTab('other')).toBe('watched')
  })

  it('builds shareable query string', () => {
    expect(
      buildDeepLinkSearch({ tab: 'registry', wagon: 'SYN-00001', date: '2024-01-15' }),
    ).toBe('?tab=registry&wagon=SYN-00001&date=2024-01-15')
  })

  it('detects explicit wagon= for card restore', () => {
    expect(hasWagonDeepLink('')).toBe(false)
    expect(hasWagonDeepLink('?tab=watched')).toBe(false)
    expect(hasWagonDeepLink('?wagon=SYN-00001')).toBe(true)
  })
})
