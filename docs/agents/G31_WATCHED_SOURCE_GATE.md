# G31 — источник «На слежении» / Active: установленное + строка разрешения

Дата: **2026-11-24**.  
База: `main` @ `e3af8c9` (после P5-E-CUR).  
Тип: **research + gate** (без CODE JDBC watched). T2 не блокирует; использовать текущую семантику времени карточки (T1 + E-CUR).  
**Не** расширяет P5-E / E-CUR CONTROL.

Evidence base: [11-nuff-active-wagon-list.md](../analysis/11-nuff-active-wagon-list.md), [08_GAPS G31](../analysis/08_GAPS_AND_UNCERTAINTIES.md), [49](../architecture/49_NK1_DT1_G31_OFFLINE_RESEARCH.md), [38](../architecture/38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md), P5-D honesty.

---

## 1. Установленный источник (честные grades)

### Кандидат Active (список «активных» вагонов для Remoting)

| Шаг | Объект | Grade |
| --- | --- | --- |
| Remoting | `IGivingDepartureCall.GetActiveWagons(sessionCode)` → `DataTable` | **CONFIRMED** |
| Server | `GivingDepartureLoader.GetActiveWagonList` | **CONFIRMED** |
| Oracle | `WagonPackage.GetActiveWagonList` — **только** `result OUT Cursor`; клиентских filter-аргументов **нет** | **CONFIRMED** |
| UI НУВ «На слежении» → этот API | call-site в WagonApp/WagonLib/PlannerLib/WagonPlanner **не найден** | **UNKNOWN** |
| Предикат membership (SQL/таблица/флаг) | BODY не виден RO | **UNKNOWN** |
| Привязка к user / client / group | на клиенте только `sessionCode` (выбор соединения); логика, если есть, **внутри BODY** | **UNKNOWN** |

### Что это **не** является (CONFIRMED exclusions)

| Кандидат | Почему нельзя подменять «слежение» |
| --- | --- |
| Все `WAGON` / полный парк | Нет доказательства = Active |
| `WagonPackage.GetWagonList` | Это «Обозреватель» / «Все вагоны» — **другой** список |
| Synthetic FIXTURE 60 | Demo honesty (P5-D); **≠** legacy Active |
| `wagons.txt` / PublishRegistry | Другой контур публикации; **не** построен из Active в найденных путях |
| E-CUR / CONTROL card SELECT | Карточка одного вагона; **не** membership списка |

Платформа сейчас: `synthetic`→`FIXTURE`; `dbtc-ro`→`SOURCE_NOT_CONNECTED` (пустой stub); ошибки не должны маскироваться под «пустой парк» при будущем JDBC.

---

## 2. Условия отбора (что известно / чего нет)

| Тема | Статус |
| --- | --- |
| Условия активности | **UNKNOWN** до BODY `GetActiveWagonList` |
| Исключения | **UNKNOWN** |
| User / client / group | **UNKNOWN** на сервере; клиент filter не передаёт |
| Дубликаты | политика после курсора — **UNKNOWN** (retain vs distinct) |
| Колонки курсора | **UNKNOWN** до SPEC/прогона (отдельный gate) |

**Вывод:** JDBC «реальный список На слежении» **нельзя** честно реализовать, пока нет SPEC/BODY (или owner-approved alternate source). Иначе риск выдать весь парк / GetWagonList под видом слежения.

---

## 3. Точечный запрос артефактов (не общий план)

Минимум для закрытия предиката:

1. **`DBTC.WAGONPACKAGE` PACKAGE SPEC** (или export) — наличие `GetActiveWagonList` / контраст `GetWagonList`  
2. **`DBTC.WAGONPACKAGE` PACKAGE BODY** (или вырезка только `GetActiveWagonList`) — таблицы/WHERE/флаги membership  

Способ: grant `ALL_SOURCE`/`ALL_ARGUMENTS` **или** file-share export (как [38](../architecture/38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md)).  
**Запрещено этим запросом:** `EXECUTE GetActiveWagonList`; выгрузка полного Active-курсора без отдельной строки; DML/DDL.

Опционально (если UI call-site критичен продукту): поиск caller вне поставки НУВ ClickOnce — **не** заменяет BODY.

---

## 4. Строки APPROVALS (для утверждения владельцем)

Сейчас в журнале: **DENIED UNTIL APPROVED**. Два последовательных gate — **не** молчаливое расширение E-CUR.

### Gate 0 — discovery (SPEC/BODY)

| Решение | Статус (сейчас) | Детали |
| --- | --- | --- |
| G31 WAGONPACKAGE SPEC/BODY export (GetActiveWagonList) | **DENIED UNTIL APPROVED** | Владелец (черновик 2026-11-24): **только** read/export текста `DBTC.WAGONPACKAGE` SPEC (+ BODY если policy) для `GetActiveWagonList` (и для контраста `GetWagonList`); via `ALL_SOURCE`/`ALL_ARGUMENTS` grant **или** secured file-share → gitignored `_oracle_tmp`. **Запрещено:** EXECUTE процедуры; materialize Active cursor; registry JDBC; E-CUR expand; writes; Create RAILDB; auto-merge; закрытие G31 без evidence. Deliverable note: [G31_WATCHED_SOURCE_GATE.md](G31_WATCHED_SOURCE_GATE.md) |

### Gate 1 — JDBC watched list (после известного предиката)

| Решение | Статус (сейчас) | Детали |
| --- | --- | --- |
| P5-G31 JDBC watched list + card drill-down | **DENIED UNTIL APPROVED** | Владелец (черновик 2026-11-24): **отдельная** строка **после** Gate 0 evidence (BODY/predicate CONFIRMED или owner-picked alternate). **Разрешено после APPROVED:** Java/React; server-side page/search/sort/total на membership-источнике из BODY (**не** все WAGON); drill-down в уже реализованную карточку (паспорт + currentState E-CUR + A1 сутки; текущая семантика времени T1/E-CUR); Oracle→API→UI smoke time-boxed; synthetic default + FIXTURE honesty сохраняются; read error ≠ empty list. **Запрещено:** client-side full-park pagination; silent E-CUR/P5-E expand; EXECUTE без отдельного allow; writes/import/change membership; T2; закрытие DT1/R1-Q-TZ без evidence; auto-merge. Contour/user/timeout/objects — **заполнить из BODY** в момент approve |

---

## 5. После Gate 0+1 (реализация — отдельный PR, не сейчас)

Целевое поведение (приёмка из запроса владельца):

- реальный список «На слежении» + total;  
- server search / sort / page;  
- переход в карточку с паспортом, currentState, историей;  
- parity количества/состава со старой системой (тот же user/момент) — **после** известного источника;  
- нет дублей (политика по evidence);  
- ошибки источника явные; synthetic работает.

**Вне скоупа:** запись, импорт, изменение состава слежения.

---

## 6. Статус сейчас

| Item | |
| --- | --- |
| Источник API Active | `WagonPackage.GetActiveWagonList` — **CONFIRMED** Remoting |
| UI «На слежении» = Active | **не доказано** |
| Предикат отбора | **UNKNOWN** — нужен BODY |
| CODE JDBC watched | **BLOCKED** до Gate 0 (+ Gate 1 approve) |
| Следующий шаг владельца | Approve Gate 0 **или** предоставить уже выгруженный SPEC/BODY |
