import { expect, test } from '@playwright/test'

/**
 * Scale UX smoke with mocked totalElements ≥ 18k.
 * Does not require a live 18k backend fixture; REST contract shape unchanged.
 */
test.describe('P4-1 @scale pagination UX (mocked totalElements)', () => {
  test('@scale registry showTotal and last page stay server-paged', async ({ page }) => {
    await page.route('**/api/v1/wagons?*', async (route) => {
      const url = new URL(route.request().url())
      if (url.pathname.endsWith('/watched')) {
        await route.continue()
        return
      }
      const pageIndex = Number(url.searchParams.get('page') ?? '0')
      const size = Number(url.searchParams.get('size') ?? '50')
      const totalElements = 18_000
      const start = pageIndex * size
      const content =
        start < totalElements
          ? [{ wagonCode: `SYN-${String(start + 1).padStart(5, '0')}` }]
          : []
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content,
          page: pageIndex,
          size,
          totalElements,
          totalPages: Math.ceil(totalElements / size),
        }),
      })
    })

    await page.goto('/?tab=registry&wagon=SYN-00001&date=2024-01-15')
    await expect(page.getByRole('tab', { name: 'Реестр вагонов' })).toHaveAttribute(
      'aria-selected',
      'true',
    )

    await expect(page.getByText(/из 18000 · стр\. 1 из 360/)).toBeVisible()

    const jumper = page.locator('.ant-pagination-options-quick-jumper input')
    await jumper.fill('360')
    await jumper.press('Enter')
    await expect(page.getByText(/из 18000 · стр\. 360 из 360/)).toBeVisible()

    // Grid still holds only the current page rows (server-page model), not 18k.
    const cells = page.getByRole('tabpanel', { name: 'Реестр вагонов' }).getByRole('gridcell')
    await expect(cells).toHaveCount(1)
  })
})
