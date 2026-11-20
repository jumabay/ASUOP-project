# 46. UI scale UX proposal — реестр / карточка под ~18k вагонов

Статус: **docs-only / PROPOSAL**. Не реализация.  
Автор среза: Frontend Developer (offline readiness review).  
Дата: 2026-11-20.

Связанные источники:

| Артефакт | Роль |
| --- | --- |
| [14_FRONTEND_ARCHITECTURE.md](14_FRONTEND_ARCHITECTURE.md) | Каркас, карта экранов, запрет скана ленты в реестре |
| [ADR-0005](decisions/ADR-0005-react-antd-ag-grid.md) | React + Ant Design + AG Grid Community; целевой реестр ~18k |
| [41_P4_0_R1_R3_FIELD_MAPPING.md](41_P4_0_R1_R3_FIELD_MAPPING.md) | REST page/size/search; synthetic = contains |
| [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md) | R1–R3 APPROVED synthetic; JDBC/repairs DENIED |
| [ASUOP_NEXT_DEVELOPMENT_STAGE.md](../agents/ASUOP_NEXT_DEVELOPMENT_STAGE.md) | UX-долг: deep-link, debounce, retry |
| Код FE | `frontend/src/App.tsx`, `WagonRegistryGrid`, `WatchedWagonGrid`, `WagonCardPanel`, `StateReportGrid`, `api/wagonsApi.ts` |
| Код BE (контракт) | `WagonRegistryCriteria` DEFAULT 50 / MAX 200 |

**Hard bans соблюдены:** без rewrite UI, без смены бизнес-логики, без Oracle, без секретов.

---

## 1. Verdict (executive)

Текущий UI **архитектурно готов к ~18k по модели данных** (серверная страница ≤200 + `totalElements`, сетка держит только текущую страницу). Он **не готов по операторскому UX** при частых наборах номера и при росте synthetic/реального каталога: поиск без debounce, нет явного «найти» для списка, нет deep-link, карточка всегда тянет сутки вместе с паспортом, статус слежения на карточке отсутствует, навигация к ремонтам/деталям — только будущий контур.

**Рекомендация lead:** инкрементальный UX-пакет поверх существующих R1–R3 контрактов; отдельно — stress-fixture ~18k под offline smoke; экраны repairs/parts — только после product APPROVED + API.

---

## 2. As-is: search / filter / paging vs AG Grid practices

### 2.1. Что уже сделано правильно

| Практика (AG Grid / large registry) | Факт в коде |
| --- | --- |
| Не грузить весь каталог в браузер | Да: `fetchWagonRegistry` / `fetchWatchedWagons` с `page`/`size`; в grid только `content` страницы |
| Жёсткий потолок page size | Да: API `1…200` (`MAX_SIZE`); UI options `50/100/200` |
| Сортировка на сервере | Да: `sort=code,asc\|desc` |
| Поиск номера на сервере | Да: query `search` (synthetic = **contains**) |
| DOM-виртуализация строк страницы | Да: AG Grid Community default row virtualization на `rowData` страницы |
| История суток отдельно от реестра | Да: отдельный `GET …/state-reports?date=`; реестр не читает ленту |
| Лимит суток без тихой обрезки | Да: UI обрабатывает `RESULT_LIMIT_EXCEEDED` (500 строк) |

### 2.2. Gaps относительно AG Grid / операторского UX

| Gap | Почему важно при ~18k | Severity |
| --- | --- | --- |
| **Нет debounce** на `Input` поиска списка: каждый символ → `setState` → `useEffect` → HTTP | При реальном JDBC — шквал LIKE-запросов; даже на synthetic — лишний churn | **High** |
| Поиск списка **не требует Enter / кнопки** «Применить» | Нет контроля момента запроса; сложно отменить набор | High |
| **Нет `showTotal` / «страница X из Y»** в `Pagination` | Оператор не видит масштаб (18 000 / 360 страниц при size=50) | Medium |
| AG Grid **без** `defaultColDef.filter` / floating filter | Согласовано с server-page моделью (client filter ввёл бы ложное «фильтр всего реестра»). Но и **быстрого колоночного фильтра на сервере** нет | Medium (by design) |
| **Client Quick Filter** AG Grid не используется | Правильно не включать на server-page; нельзя путать с серверным `search` | Info |
| **Infinite / Server-Side Row Model** не используются | Community Infinite возможен; SSRM — Enterprise. Сейчас Ant `Pagination` + client row model на странице — **допустимый** Community-паттерн | Low (ok) |
| Фиксированная высота grid `360`/`420` | При size=200 скролл внутри grid ок; на широком мониторе мало «воздуха» | Low |
| `animateRows` включён | На 200 строках лишняя работа; при 18k не критично (страница мала) | Low |
| Реестр: **одна колонка** `wagonCode` | При 18k оператору часто нужны род/собственник/станция — сейчас они только в watched / card | Medium (product) |
| Выбор строки сразу зовёт **card + day** | Лишняя нагрузка, если нужна только карточка; день зависит от DatePicker | Medium |
| Поиск карточки (нижний блок) **не связан** с глобальным поиском из [14] | В 14 — «глобальный поиск → GetWagonByCode»; сейчас локальный Input | Medium (shell gap) |
| Watched: synthetic **60** кодов; registry synthetic **250** | Offline не доказывает UX/paging при 18k | High (test gap) |
| Нет retry-кнопки на ошибке списка (есть только «Обновить») | Уже отмечено как UX-долг в next-stage | Low |
| Нет deep-link URL (`?wagon=&date=&tab=`) | Нельзя шарить/обновлять F5 с контекстом | Medium |

### 2.3. Модель строк AG Grid (рекомендуемая позиция)

**Оставить** Community + **внешняя** Ant Pagination + client `rowData` = текущая страница.

Не предлагать AG Grid Enterprise SSRM без отдельного ADR/APPROVAL.  
Infinite Row Model — опциональный later-step; не обязателен, если paging + debounce закрывают 18k.

**Запретить** в UI:

- загрузку `totalElements` строк в grid;
- client-only filter/sort как будто это весь парк;
- снятие `MAX_SIZE=200` «для удобства».

---

## 3. Proposal (incremental only — не redesign)

Приоритеты: **P0** = можно планировать сразу под synthetic; **P1** = UX после/вместе с JDBC; **P2** = future modules.

### 3.1. Поиск по номеру вагона

| Шаг | Поведение | Контракт |
| --- | --- | --- |
| P0 | Debounce **250–400 ms** на `search` списков; сброс на page=0 (уже есть) | Без смены API |
| P0 | Enter / кнопка «Найти» = немедленный запрос; Clear = пустой search + reload | Без смены API |
| P0 | Блок карточки: оставить Enter + «Найти» (уже есть); не автозагрузка на каждый символ | Без смены API |
| P1 | Уточнить predicate: **prefix** или **exact** для промышленного номера (сейчас synthetic contains) | Может потребовать согласования с JDBC mapping ([41] PROVISIONAL) + строка APPROVALS если меняется семантика REST |
| P1 | Глобальный поиск в шапке → открыть карточку (`GetWagonByCode` / `GET /wagons/{code}`) по [14] | FE shell; API уже есть (R1) |
| P2 | Typeahead / suggest «SYN-00…» | **Новый** endpoint — нужен APPROVAL |

### 3.2. Быстрый фильтр

| Шаг | Поведение | Контракт |
| --- | --- | --- |
| P0 | Один серверный `search` по номеру — единственный «быстрый фильтр» списков | Существующий |
| P0 | **Не** включать AG Grid Quick Filter / column filter на странице без server binding (ложная полнота) | FE only |
| P1 | Опционально: server `search` + hint «фильтр по подстроке номера, не по модели» | Docs/UI copy |
| P1 | Расширить колонки реестра до passport-полей (как watched) **только если** backend начнёт отдавать их в `WagonRegistryItem` | **Смена DTO** → backend APPROVAL; сейчас item = только `wagonCode` |
| P2 | Фильтры по роду / собственнику / станции | Новые query params + индексы — отдельный slice |

### 3.3. История суток (day history)

| Шаг | Поведение | Контракт |
| --- | --- | --- |
| P0 | Разделить действия: «Открыть карточку» vs «Загрузить сутки» (или грузить сутки только по явной кнопке / смене даты) | Те же R1 API; меньше лишних day-запросов |
| P0 | При смене DatePicker не автозагружать, пока нет явного действия (или debounce+confirm) | FE |
| P0 | Сохранить Alert на `RESULT_LIMIT_EXCEEDED` / empty day / not found | Уже есть |
| P1 | Показ числа строк суток + «лимит 500» в подписи grid | FE copy |
| P1 | Client column sort/filter **внутри** уже загруженных ≤500 строк — допустим (набор мал) | FE only |
| P2 | Пагинация/сужение суток на сервере при >500 | **Новый** контракт / смена политики лимита — APPROVAL |

### 3.4. Статус слежения (watched)

| Шаг | Поведение | Контракт |
| --- | --- | --- |
| P0 | На вкладке watched — badge/подпись «на слежении» уже контекстом вкладки; при клике открывать карточку | Есть |
| P0 | На карточке: если `wagonCode` был выбран из watched tab — показать Tag «На слежении» (client hint) | FE only; **не** утверждать legacy parity (G31 OPEN) |
| P1 | Поле `watched: boolean` (или отдельный `HEAD`/`GET` membership) на карточке | **Расширение R1 DTO** → APPROVAL; membership UNKNOWN до G31 |
| P1 | Deep-link `?tab=watched` | FE |
| — | Не обещать соответствие `GetActiveWagonList` | R3 / R4 evidence |

### 3.5. Навигация к ремонтам / деталям (future)

| Шаг | Поведение | Контракт |
| --- | --- | --- |
| P0 | В карточке: disabled пункты «Ремонты», «Склад / детали» с tooltip «появится после утверждения модуля» | FE shell only; по [14] пункты без теста A недоступны |
| P0 | Не рисовать пустые формы repairs/warehouse | Запрет [14] |
| P2 | Read-only список ремонтов / остаток детали из карточки | Нужны API A3–A8 + product APPROVED (см. next-stage S7 / P2-4) |

Навигационный контракт (черновик для lead, не API):

```
Карточка вагона {code}
  → (future) /repairs?wagon={code}
  → (future) /warehouse/parts?wagon={code}   // или через id ремонта — UNKNOWN до research
```

---

## 4. Offline under R1–R3 APPROVED vs new APPROVALS / APIs

### 4.1. Можно делать offline **без** новых APPROVALS (в рамках существующих R1–R3 + P0-5 / P1-4)

Ограничение: **не менять** бизнес-семантику REST; только FE UX / тесты / docs; synthetic profile.

| Работа | Обоснование |
| --- | --- |
| Debounce + Enter/«Найти» / Clear для registry & watched search | Существующие query params |
| `Pagination.showTotal`, keyboard focus, taller grid, отключить `animateRows` | FE polish |
| Разделение загрузки card vs day; retry Alert actions | Те же endpoints |
| Deep-link query string (wagon, date, tab) | FE router/state; API unchanged |
| Disabled future nav stubs на карточке | UI only |
| Client Tag «открыто из слежения» | Не утверждает membership в БД |
| Playwright/Vitest на текущих 250/60 | P1-4 already APPROVED |
| Docs (этот файл) | Явный запрос lead |

### 4.2. Можно, но нужен **явный** узкий APPROVAL (synthetic/test infrastructure)

| Работа | Почему отдельная строка |
| --- | --- |
| Увеличить synthetic registry fixture до **~18 000** кодов (+ опционально watched subset) для offline scale smoke | Не Oracle; но меняет объём fixture/CI время; лучше отдельная строка «synthetic scale fixture» |
| Стресс-тест paging (360 страниц × size 50) в CI | Нагрузка на pipeline; лимиты времени |

**Не** снимает `@Disabled` JDBC parity и **не** открывает DataSource.

### 4.3. Требует **новых APPROVALS** и/или **новых / изменённых API**

| Работа | Блокер |
| --- | --- |
| JDBC/Oracle/RAILDB read adapters, реальные 18k | TEST/RAILDB URL; Accept ADR-0009 path; строки JDBC |
| Смена семантики `search` (contains → prefix/exact) в контракте | Согласование 41 + backend; строка если «R1–R3 contracts не менять» нарушается |
| Поля passport в `WagonRegistryItem` | Смена DTO R2 |
| `watched` flag на `WagonCard` | Смена DTO R1; G31 |
| Typeahead / multi-field filters | Новые API |
| Пагинация суток / лимит ≠ 500 | Смена R1 day policy |
| Экраны ремонтов, склада, деталей, Excel export server | A3–A8 / reports; DENIED until product APPROVED |
| AG Grid Enterprise / SSRM | ADR + cost APPROVAL |
| Live FTP/RWL, Production | DENIED |

### 4.4. Матрица «фича → gate»

| Фича | Offline FE now | Synthetic fixture 18k | New API | Oracle |
| --- | --- | --- | --- | --- |
| Debounce search | ✅ | — | — | — |
| showTotal / deep-link | ✅ | — | — | — |
| Card vs day split load | ✅ | — | — | — |
| Watched Tag (from tab) | ✅ | — | — | — |
| Scale smoke 18k pages | — | ✅ (APPROVAL) | — | — |
| Registry rich columns | — | optional | ✅ DTO | later |
| Prefix search semantics | — | can emulate | ✅ contract | ✅ JDBC |
| Watched on card (truth) | — | partial | ✅ + G31 | ✅ |
| Repairs / parts nav live | stub only | — | ✅ modules | ✅ |

---

## 5. Acceptance sketch (когда lead разрешит реализацию UX-пакета)

Не реализуется этим документом. Черновик критерия для будущего FE slice:

1. При вводе в поиск списка уходит **не больше одного** запроса на паузу debounce (или Enter).
2. При `totalElements` ≥ 10 000 UI показывает total и не пытается загрузить все строки.
3. Page size > 200 отвергается сервером; UI не предлагает больших значений.
4. Открытие карточки из реестра/слежения не ломает R1 error states (404 / 422 day).
5. Пункты ремонтов/склада либо disabled, либо отсутствуют — без пустых форм.
6. Тесты: Vitest на debounce/paging UX; Playwright smoke (P1-4) без секретов.
7. При наличии scale-fixture: пройти «последняя страница» и search, сужающий набор.

---

## 6. Risks / open questions for lead

1. **G31 OPEN** — нельзя обещать оператору «официальный» список слежения = legacy Active.
2. **Search predicate** для номеров 8 цифр vs `SYN-#####`: contains на 18k в Oracle может быть дорогим — нужен prefix/index план до JDBC UI-тюнинга.
3. Нужен ли **rich registry** (колонки как watched) до JDBC, или оставить тонкий R2 item?
4. Разрешать ли **synthetic 18k fixture** в CI или только локальный opt-in профиль?
5. Глобальный поиск в шапке — часть ближайшего UX-пакета или ждать app shell (меню модулей)?

---

## 7. Recommended next approval (одно решение)

Предлагаемая строка в APPROVALS (текст для владельца):

> **FE UX scale pack (offline, R1–R3 contracts unchanged):** debounce/Enter search, pagination totals, card/day load split, deep-link, disabled future nav stubs, tests. Optional separate line: **synthetic registry fixture ~18k** for offline paging smoke. **Out of scope:** Oracle/JDBC, DTO changes, repairs/warehouse screens, Enterprise AG Grid.

До этой строки код UI **не** менять (этот документ — только proposal).

---

## 8. Files reviewed (evidence)

| Path | Observation |
| --- | --- |
| `frontend/src/App.tsx` | Tabs watched/registry; search onChange → immediate state; Pagination 50/100/200; card+day coupled in `loadWagonDetails` |
| `frontend/src/components/WagonRegistryGrid.tsx` | Single column; client rowData; height 360 |
| `frontend/src/components/WatchedWagonGrid.tsx` | Passport columns; height 420 |
| `frontend/src/components/WagonCardPanel.tsx` | Descriptions only; no watched/repairs |
| `frontend/src/components/StateReportGrid.tsx` | Full day lines in memory (≤500 by API) |
| `frontend/src/api/wagonsApi.ts` | page/size/sort/search; card; state-reports |
| `modules/fleet/.../WagonRegistryCriteria.java` | DEFAULT_SIZE 50, MAX_SIZE 200 |
| Synthetic IT | registry `totalElements=250`, watched `=60` |

---

## 9. Status hand-off

| Item | Value |
| --- | --- |
| Screens assessed | Watched list, wagon registry, wagon card, day history |
| APIs assumed | `GET /api/v1/wagons`, `/watched`, `/{code}`, `/{code}/state-reports` |
| Implementation | **None** (proposal only) |
| Tests run | N/A (docs-only) |
| Legacy comparison | Watched ≠ `GetActiveWagonList` (UNKNOWN); registry must not scan dislocation tape ([14]) |
| Risks | Debounce gap; fixture << 18k; G31; search predicate cost on JDBC |
| Next approval needed | FE UX scale pack (+ optional synthetic 18k fixture) per §7 |
