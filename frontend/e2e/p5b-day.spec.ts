import { expect, test, type Page, type Route } from '@playwright/test'

const SYN_WAGON = 'SYN-00001'
const DAY_WITH_DATA = '2024-01-15'
const DAY_EMPTY = '2024-01-16'

const emptyPage = {
  content: [] as unknown[],
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0,
}

const sampleCard = {
  wagonCode: SYN_WAGON,
  model: '12-132',
  wagonKind: 'КРЫТЫЙ',
  ownerName: 'SYN-OWNER',
  homeStationCode: '6900',
  homeStationName: 'SYN-STATION',
  inventoryPark: true,
}

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  })
}

/** Offline API stubs so P5-B day UX e2e does not require bootRun. */
async function stubBaseApi(page: Page) {
  await page.route('**/api/v1/wagons/**', async (route) => {
    const url = route.request().url()
    if (url.includes('/state-reports')) {
      // Overridden per-test when needed; default success with data
      await fulfillJson(route, {
        wagonCode: SYN_WAGON,
        date: DAY_WITH_DATA,
        lines: [
          {
            reportId: 1001,
            wagonCode: SYN_WAGON,
            snapshotTime: '2024-01-15T08:00:00',
            dislocationTime: '2024-01-15T08:00:00',
            stationCode: '6900',
            operationCode: '01',
          },
        ],
      })
      return
    }
    if (url.includes('/watched')) {
      await fulfillJson(route, emptyPage)
      return
    }
    // card: /api/v1/wagons/{code}
    await fulfillJson(route, sampleCard)
  })

  await page.route('**/api/v1/wagons?**', async (route) => {
    await fulfillJson(route, emptyPage)
  })
  await page.route('**/api/v1/wagons', async (route) => {
    await fulfillJson(route, emptyPage)
  })
}

async function setPickerDate(page: Page, isoDate: string) {
  const input = page.locator('.ant-picker-input input').first()
  await input.click()
  await input.fill(isoDate)
  await input.press('Enter')
  await page.keyboard.press('Escape')
}

test.describe('P5-B day UX (loading / empty / error / retry)', () => {
  test.beforeEach(async ({ page }) => {
    await stubBaseApi(page)
    await page.goto('/')
    await expect(page.getByText('АСУВП')).toBeVisible()
  })

  test('loads day lines and shows N / max 500 caption', async ({ page }) => {
    await page.getByPlaceholder('Номер вагона').fill(SYN_WAGON)
    await setPickerDate(page, DAY_WITH_DATA)
    await page.getByRole('button', { name: 'Загрузить сутки' }).click()

    await expect(page.getByText(`История дислокации · ${DAY_WITH_DATA}`)).toBeVisible()
    await expect(page.getByTestId('day-report-line-count')).toHaveText('1 / max 500')
    await expect(page.getByRole('gridcell', { name: SYN_WAGON, exact: true }).first()).toBeVisible()
  })

  test('empty day keeps Empty copy and 0 / max 500', async ({ page }) => {
    await page.unroute('**/api/v1/wagons/**')
    await page.route('**/api/v1/wagons/**', async (route) => {
      const url = route.request().url()
      if (url.includes('/state-reports')) {
        await fulfillJson(route, { wagonCode: SYN_WAGON, date: DAY_EMPTY, lines: [] })
        return
      }
      if (url.includes('/watched')) {
        await fulfillJson(route, emptyPage)
        return
      }
      await fulfillJson(route, sampleCard)
    })

    await page.getByPlaceholder('Номер вагона').fill(SYN_WAGON)
    await setPickerDate(page, DAY_EMPTY)
    await page.getByRole('button', { name: 'Загрузить сутки' }).click()

    await expect(page.getByText('За выбранные сутки записей нет')).toBeVisible()
    await expect(page.getByTestId('day-report-line-count')).toHaveText('0 / max 500')
  })

  test('day error shows Повторить and recovers on retry', async ({ page }) => {
    let dayCalls = 0
    await page.unroute('**/api/v1/wagons/**')
    await page.route('**/api/v1/wagons/**', async (route) => {
      const url = route.request().url()
      if (url.includes('/state-reports')) {
        dayCalls += 1
        if (dayCalls === 1) {
          await fulfillJson(
            route,
            { error: 'UPSTREAM_UNAVAILABLE', message: 'Временная ошибка суток' },
            503,
          )
          return
        }
        await fulfillJson(route, {
          wagonCode: SYN_WAGON,
          date: DAY_WITH_DATA,
          lines: [
            {
              reportId: 1001,
              wagonCode: SYN_WAGON,
              // intentional: no snapshotTime — UI falls back to dislocationTime
              dislocationTime: '2024-01-15T08:00:00',
              stationCode: '6900',
              operationCode: '01',
            },
          ],
        })
        return
      }
      if (url.includes('/watched')) {
        await fulfillJson(route, emptyPage)
        return
      }
      await fulfillJson(route, sampleCard)
    })

    await page.getByPlaceholder('Номер вагона').fill(SYN_WAGON)
    await setPickerDate(page, DAY_WITH_DATA)
    await page.getByRole('button', { name: 'Загрузить сутки' }).click()

    await expect(page.getByTestId('day-report-error')).toBeVisible()
    await expect(page.getByText('Временная ошибка суток')).toBeVisible()

    await page.getByTestId('day-report-retry').click()
    await expect(page.getByText(`История дислокации · ${DAY_WITH_DATA}`)).toBeVisible()
    await expect(page.getByTestId('day-report-line-count')).toHaveText('1 / max 500')
  })

  test('preserves 422 limitExceeded messaging without silent trim', async ({ page }) => {
    await page.unroute('**/api/v1/wagons/**')
    await page.route('**/api/v1/wagons/**', async (route) => {
      const url = route.request().url()
      if (url.includes('/state-reports')) {
        await fulfillJson(
          route,
          {
            error: 'RESULT_LIMIT_EXCEEDED',
            message: 'too many',
            wagonCode: SYN_WAGON,
            date: DAY_WITH_DATA,
          },
          422,
        )
        return
      }
      if (url.includes('/watched')) {
        await fulfillJson(route, emptyPage)
        return
      }
      await fulfillJson(route, sampleCard)
    })

    await page.getByPlaceholder('Номер вагона').fill(SYN_WAGON)
    await setPickerDate(page, DAY_WITH_DATA)
    await page.getByRole('button', { name: 'Загрузить сутки' }).click()

    await expect(page.getByText('RESULT_LIMIT_EXCEEDED')).toBeVisible()
    await expect(
      page.getByText('Строк больше 500. Сервер вернул HTTP 422 без обрезки данных.'),
    ).toBeVisible()
    await expect(page.getByTestId('day-report-retry')).toHaveCount(0)
  })

  test('list error shows Повторить and reloads registry', async ({ page }) => {
    let registryCalls = 0
    await page.route('**/api/v1/wagons?**', async (route) => {
      registryCalls += 1
      if (registryCalls === 1) {
        await fulfillJson(route, { error: 'LIST_ERROR', message: 'Ошибка реестра' }, 500)
        return
      }
      await fulfillJson(route, {
        content: [{ wagonCode: SYN_WAGON }],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      })
    })
    await page.route('**/api/v1/wagons', async (route) => {
      registryCalls += 1
      if (registryCalls <= 1) {
        await fulfillJson(route, { error: 'LIST_ERROR', message: 'Ошибка реестра' }, 500)
        return
      }
      await fulfillJson(route, {
        content: [{ wagonCode: SYN_WAGON }],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      })
    })

    await page.getByRole('tab', { name: 'Реестр вагонов' }).click()
    await expect(page.getByTestId('list-error-retry')).toBeVisible()
    await page.getByTestId('list-error-retry-button').click()
    await expect(page.getByRole('gridcell', { name: SYN_WAGON, exact: true })).toBeVisible()
  })
})
