# Готовность карточки вагона (после P5-E-CUR)

Дата: **2026-11-24**.  
Итоговый `main`: **`c23833a45b493ce9664c5424e884da4d3b12fb3d`**  
(`Merge pull request #39 from jumabay/feature/p5-e-cur-wagon-current-state`)

Post-merge offline smoke @ `c23833a` (synthetic only, not Oracle): Gradle **148** tests PASS (1 skipped); Vitest **51/51**; `npm run build` OK.

## Реализовано (read path)

| Блок | Источник | Статус |
| --- | --- | --- |
| **Паспорт** A2 | `WAGON` + refs (models/types/owners/home) | **DONE** (P5-E + E-CUR retains) |
| **currentState** | **только** master `WAGON` (+ soft `STATIONS`/`OPERATIONS`) | **DONE** (P5-E-CUR) |
| **История срезов** A1 | `STATEREPORTRECORDS` по `GETTING_DATE` (сутки) | **DONE** (P5-E; T1 `snapshotTime`) |

## Поля `currentState`

| Поле | Реализация | Примечание |
| --- | --- | --- |
| `stationCode` | `LOCATIONSTATION_ID` → `STATIONS.CODE` | soft join; orphan → null |
| `operationCode` | `IDCODE` → `CODE` (string) | ведущие нули сохраняются |
| `dislocationTime` | **только** `LOCATIONDATE` | семантика **PROVISIONAL** (не «факт» CONFIRMED) |
| `trainIndex` | **всегда `null`** | `TRAINCODE` на WAGON обнаружен; маппинг **не** разрешён |

UI dbtc-ro: «Дата положения по данным источника» (не ось суток).

## Не закрыто

| Item | Status |
| --- | --- |
| Семантика времени current / DT1 | **PROVISIONAL** / **OPEN** |
| R1-Q-TZ | **OPEN** |
| G31 / NK1 | **OPEN** |
| T2 `locationTime` | **DENIED** |
| registry / watched JDBC | **out of scope** E-CUR |

## Evidence

- Live CONTROL dbtc-ro: [P5_E_CUR_CONTROL_EVIDENCE.md](P5_E_CUR_CONTROL_EVIDENCE.md)  
- Offline synthetic: [P5_E_CUR_OFFLINE_EVIDENCE.md](P5_E_CUR_OFFLINE_EVIDENCE.md)  
- Synthetic e2e **≠** проверка Oracle-профиля.
