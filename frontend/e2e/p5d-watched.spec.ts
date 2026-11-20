import { expect, test, type Page } from '@playwright/test'

const SYN_WAGON = 'SYN-00001'

async function applyListSearch(page: Page, panelName: string, value: string) {
  const panel = page.getByRole('tabpanel', { name: panelName })
  const search = panel.getByPlaceholder('Поиск по номеру (contains)')
  await search.fill(value)
  await panel.getByRole('button', { name: 'Найти' }).click()
}

test.describe('P5-D watched membership honesty (synthetic)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await expect(page.getByText('АСУВП')).toBeVisible()
  })

  test('FIXTURE banner states demo-only, not company Active list', async ({ page }) => {
    await expect(page.getByRole('tab', { name: 'Вагоны на слежении' })).toHaveAttribute(
      'aria-selected',
      'true',
    )

    const banner = page.getByTestId('watched-membership-banner')
    await expect(banner).toBeVisible()
    await expect(banner).toHaveAttribute('data-membership-source', 'FIXTURE')
    await expect(banner).toContainText(/FIXTURE|демо/i)
    await expect(banner).toContainText(/G31/i)

    const watchedJson = await page.request.get('/api/v1/wagons/watched?size=1')
    expect(watchedJson.ok()).toBeTruthy()
    const body = await watchedJson.json()
    expect(body.membershipSource).toBe('FIXTURE')
    expect(String(body.membershipNote)).toMatch(/FIXTURE/)
  })

  test('card tag from watched is navigation context only', async ({ page }) => {
    await applyListSearch(page, 'Вагоны на слежении', SYN_WAGON)
    const panel = page.getByRole('tabpanel', { name: 'Вагоны на слежении' })
    const row = panel.getByRole('row', { name: new RegExp(SYN_WAGON) })
    await expect(row).toBeVisible()
    await row.click()

    const tag = page.getByTestId('wagon-card-watched-tag')
    await expect(tag).toHaveText('Контекст: слежение')
    await expect(tag).not.toHaveText('На слежении')
  })

  test('EMPTY_PENDING_G31 honesty when API returns empty rail source', async ({ page }) => {
    await page.route('**/api/v1/wagons/watched*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          page: 0,
          size: 50,
          totalElements: 0,
          totalPages: 0,
          membershipSource: 'EMPTY_PENDING_G31',
          membershipNote:
            'Подтверждённый источник членства слежения отсутствует (G31 OPEN). Список пуст.',
        }),
      })
    })

    await page.goto('/?tab=watched')
    const banner = page.getByTestId('watched-membership-banner')
    await expect(banner).toBeVisible()
    await expect(banner).toHaveAttribute('data-membership-source', 'EMPTY_PENDING_G31')
    await expect(banner).toContainText(/G31/)
    await expect(page.getByTestId('watched-empty-pending-g31')).toBeVisible()
  })
})
