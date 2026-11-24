import { expect, test, type Page } from '@playwright/test'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const SYN_WAGON = 'SYN-00001'
const DAY_WITH_DATA = '2024-01-15'
const DAY_EMPTY = '2024-01-16'
const UNKNOWN_WAGON = 'SYN-UNKNOWN'

const screenshotsDir = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'screenshots',
)

async function setPickerDate(page: Page, isoDate: string) {
  const input = page.locator('.ant-picker-input input').first()
  await input.click()
  await input.fill(isoDate)
  await input.press('Enter')
  await page.keyboard.press('Escape')
}

async function applyListSearch(page: Page, panelName: string, value: string) {
  const panel = page.getByRole('tabpanel', { name: panelName })
  const search = panel.getByPlaceholder('Поиск по номеру (contains)')
  await search.fill(value)
  await panel.getByRole('button', { name: 'Найти' }).click()
}

function cardFindButton(page: Page) {
  return page
    .locator('.ant-card')
    .filter({ hasText: 'Карточка и история' })
    .getByRole('button', { name: 'Найти' })
}

test.describe('P1-4 synthetic UI smoke (offline)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await expect(page.getByText('АСУВП')).toBeVisible()
  })

  test('watched list loads and filters by synthetic code', async ({ page }) => {
    await expect(page.getByRole('tab', { name: 'Инвентарный парк' })).toHaveAttribute(
      'aria-selected',
      'true',
    )

    await applyListSearch(page, 'Инвентарный парк', SYN_WAGON)
    const panel = page.getByRole('tabpanel', { name: 'Инвентарный парк' })
    await expect(panel.getByRole('gridcell', { name: SYN_WAGON, exact: true })).toBeVisible()

    await applyListSearch(page, 'Инвентарный парк', 'NOMATCH')
    await expect(page.getByText('По заданным условиям вагоны не найдены')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotsDir, 'watched-empty-filter.png'),
      fullPage: true,
    })
  })

  test('registry tab loads and shows SYN-00001', async ({ page }) => {
    await page.getByRole('tab', { name: 'Реестр вагонов' }).click()
    await expect(page.getByRole('tab', { name: 'Реестр вагонов' })).toHaveAttribute(
      'aria-selected',
      'true',
    )

    await applyListSearch(page, 'Реестр вагонов', SYN_WAGON)
    const panel = page.getByRole('tabpanel', { name: 'Реестр вагонов' })
    await expect(panel.getByRole('gridcell', { name: SYN_WAGON, exact: true })).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotsDir, 'registry-syn-00001.png'),
      fullPage: true,
    })
  })

  test('wagon card loads without day; day on explicit action', async ({ page }) => {
    await page.getByPlaceholder('Номер вагона').fill(SYN_WAGON)
    await setPickerDate(page, DAY_WITH_DATA)
    await cardFindButton(page).click()

    await expect(page.getByText(`Карточка вагона ${SYN_WAGON}`, { exact: true })).toBeVisible()
    await expect(page.getByText(/История дислокации/)).toHaveCount(0)

    await page.getByRole('button', { name: 'Загрузить сутки' }).click()
    await expect(page.getByText(`История дислокации · ${DAY_WITH_DATA}`)).toBeVisible()
    await expect(page.getByRole('gridcell', { name: SYN_WAGON, exact: true }).first()).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotsDir, 'card-day-2024-01-15.png'),
      fullPage: true,
    })
  })

  test('empty day history path', async ({ page }) => {
    await page.getByPlaceholder('Номер вагона').fill(SYN_WAGON)
    await setPickerDate(page, DAY_EMPTY)
    await page.getByRole('button', { name: 'Загрузить сутки' }).click()

    await expect(page.getByText(`История дислокации · ${DAY_EMPTY}`)).toBeVisible()
    await expect(page.getByText('За выбранные сутки записей нет')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotsDir, 'day-empty-2024-01-16.png'),
      fullPage: true,
    })
  })

  test('not-found error path', async ({ page }) => {
    await page.getByPlaceholder('Номер вагона').fill(UNKNOWN_WAGON)
    await setPickerDate(page, DAY_WITH_DATA)
    await cardFindButton(page).click()

    await expect(page.getByText('Вагон не найден')).toBeVisible()

    await page.screenshot({
      path: path.join(screenshotsDir, 'wagon-not-found.png'),
      fullPage: true,
    })
  })

  test('watched row opens card only; deep-link updates URL', async ({ page }) => {
    await applyListSearch(page, 'Инвентарный парк', SYN_WAGON)
    const panel = page.getByRole('tabpanel', { name: 'Инвентарный парк' })
    const row = panel.getByRole('row', { name: new RegExp(SYN_WAGON) })
    await expect(row).toBeVisible()
    await row.click()

    await expect(page.getByText(`Карточка вагона ${SYN_WAGON}`, { exact: true })).toBeVisible()
    await expect(page.getByTestId('wagon-card-watched-tag')).toHaveText('Контекст: инвентарный парк')
    await expect(page.getByText(/История дислокации/)).toHaveCount(0)
    await expect(page).toHaveURL(/tab=watched/)
    await expect(page).toHaveURL(new RegExp(`wagon=${SYN_WAGON}`))
  })
})
