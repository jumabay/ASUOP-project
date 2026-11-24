import { expect, test } from '@playwright/test'

const SYN_WAGON = 'SYN-00001'

test.describe('P5-A shell + global search + URL list state', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await expect(page.getByTestId('app-sider-brand')).toBeVisible()
  })

  test('shell shows fleet nav enabled and module stubs disabled', async ({ page }) => {
    await expect(page.getByRole('menuitem', { name: /Инвентарный парк/ })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: /Реестр/ })).toBeVisible()
    await expect(page.getByTestId('nav-stub-repairs')).toBeVisible()
    await expect(page.getByTestId('nav-stub-warehouse')).toBeVisible()
    await expect(page.getByTestId('nav-stub-reports')).toBeVisible()
    await expect(page.getByRole('menuitem', { name: /Ремонты/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    )
    await expect(page.getByTestId('data-source-label')).toContainText(/Synthetic/)
    await expect(page.getByText('R3 · synthetic')).toHaveCount(0)
  })

  test('global header search loads card without day history', async ({ page }) => {
    const global = page.getByTestId('global-wagon-search')
    await global.getByRole('textbox', { name: 'Глобальный поиск вагона' }).fill(SYN_WAGON)
    await global.getByRole('button', { name: 'Найти' }).click()

    await expect(page.getByText(/Карточка вагона/)).toBeVisible()
    await expect(page.getByText(/История дислокации/)).toHaveCount(0)
  })

  test('F5-style URL restores list search/page/size/sort', async ({ page }) => {
    await page.goto(
      '/?tab=registry&wagon=SYN-00001&date=2024-01-15&search=SYN&page=0&size=100&sort=code,desc',
    )

    await expect(page.getByRole('tab', { name: 'Реестр вагонов' })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    const panel = page.getByRole('tabpanel', { name: 'Реестр вагонов' })
    await expect(panel.getByPlaceholder('Поиск по номеру (contains)')).toHaveValue('SYN')
    await expect(page.getByTestId('deep-link-preview')).toContainText('search=SYN')
    await expect(page.getByTestId('deep-link-preview')).toContainText('size=100')
    await expect(page.getByTestId('deep-link-preview')).toContainText('sort=code')
  })

  test('browser back restores previous fleet tab', async ({ page }) => {
    await expect(page.getByRole('tab', { name: 'Инвентарный парк' })).toHaveAttribute(
      'aria-selected',
      'true',
    )

    await page.getByRole('menuitem', { name: /Реестр/ }).click()
    await expect(page.getByRole('tab', { name: 'Реестр вагонов' })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page).toHaveURL(/tab=registry/)

    await page.goBack()
    await expect(page.getByRole('tab', { name: 'Инвентарный парк' })).toHaveAttribute(
      'aria-selected',
      'true',
    )
  })
})
