import { describe, expect, it } from 'vitest'
import {
  buildDeepLinkSearch,
  hasWagonDeepLink,
  parseListSort,
  parseListTab,
  readDeepLinkFromSearch,
} from '../utils/deepLink'

describe('deepLink query string', () => {
  it('parses tab, wagon, date and list params from search', () => {
    expect(
      readDeepLinkFromSearch(
        '?tab=registry&wagon=SYN-00042&date=2024-01-16&search=SYN&page=2&size=100&sort=code,desc',
      ),
    ).toEqual({
      tab: 'registry',
      wagon: 'SYN-00042',
      date: '2024-01-16',
      search: 'SYN',
      page: 2,
      size: 100,
      sort: 'code,desc',
    })
  })

  it('defaults missing params for F5-safe restore', () => {
    expect(readDeepLinkFromSearch('')).toEqual({
      tab: 'watched',
      wagon: 'SYN-00001',
      date: '2024-01-15',
      search: '',
      page: 0,
      size: 50,
      sort: 'code,asc',
    })
    expect(parseListTab('watched')).toBe('watched')
    expect(parseListTab('other')).toBe('watched')
    expect(parseListSort('code,desc')).toBe('code,desc')
    expect(parseListSort('bogus')).toBe('code,asc')
  })

  it('omits empty search and round-trips defaults', () => {
    const built = buildDeepLinkSearch({
      tab: 'registry',
      wagon: 'SYN-00001',
      date: '2024-01-15',
      search: '',
      page: 0,
      size: 50,
      sort: 'code,asc',
    })
    expect(built).toBe(
      '?tab=registry&wagon=SYN-00001&date=2024-01-15&page=0&size=50&sort=code%2Casc',
    )
    expect(built.includes('search=')).toBe(false)
    expect(readDeepLinkFromSearch(built)).toEqual({
      tab: 'registry',
      wagon: 'SYN-00001',
      date: '2024-01-15',
      search: '',
      page: 0,
      size: 50,
      sort: 'code,asc',
    })
  })

  it('clamps size to 200 and rejects negative page', () => {
    expect(readDeepLinkFromSearch('?size=500&page=-3')).toMatchObject({
      size: 200,
      page: 0,
    })
  })

  it('detects explicit wagon= for card restore', () => {
    expect(hasWagonDeepLink('')).toBe(false)
    expect(hasWagonDeepLink('?tab=watched')).toBe(false)
    expect(hasWagonDeepLink('?wagon=SYN-00001')).toBe(true)
  })
})
