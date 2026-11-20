# 53. P4-2 — комплект передачи DBA (completion)

Дата: **2026-11-20**.  
База: `main` @ merge PR #17 (`d656498`).  
Статус: **handoff ready for DBA** — ответы DBA **ещё не получены**.

> Checklist / docs **не** заменяют подпись DBA.  
> **Create Oracle RAILDB (TEST)** = **DENIED** ([APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md)).  
> **NK1 / DT1** = **BLOCKED**; **G31** = **OPEN**; **watched / M8** = empty until G31 ([49](49_NK1_DT1_G31_OFFLINE_RESEARCH.md)).

**Запрещено этим пакетом:** Create RAILDB, Oracle connect, Flyway, JDBC, DBTC read, live migrate, FTP/RWL, подстановка догадок вместо ответов, auto-merge, новый CODE-срез.

---

## 1. Комплект для передачи DBA

Передайте адресату **в указанном порядке**:

| # | Документ | Зачем |
| --- | --- | --- |
| 1 | [51_DBA_REQUEST_RAILDB_TEST_RU.md](51_DBA_REQUEST_RAILDB_TEST_RU.md) | Письмо-обращение на русском + контекст |
| 2 | [52_DBA_RESPONSE_FORM_G1_G10.md](52_DBA_RESPONSE_FORM_G1_G10.md) | Форма ответа G1–G10 (заполнить) |
| 3 | [43_ADR0009_DBA_CREATE_CHECKLIST.md](43_ADR0009_DBA_CREATE_CHECKLIST.md) | Полный чеклист Owner/DBA/Status |
| 4 | [48_DBA_HANDOFF_PACKAGE.md](48_DBA_HANDOFF_PACKAGE.md) | Технический handoff (topology, sizing estimates, accounts design) |
| 5 | [50_P4_2_GO_NOGO_CREATE_RAILDB_TEST.md](50_P4_2_GO_NOGO_CREATE_RAILDB_TEST.md) | Go/No-Go для владельца после ответа |
| 6 | (справка) [49](49_NK1_DT1_G31_OFFLINE_RESEARCH.md) | Не блокер пустого Create; не закрывать gaps «заодно» |

Опционально во вложении: [24](24_RAILDB_DBA_INSTRUCTION.md) (sizing math).  
SQL `sql/dba/p4-2/*` — **не** для этого шага (только после отдельного APPROVED на probes).

**Канал:** email / тикет DBA. Пароли и connection strings с секретами — **не** в Git и не в публичный чат.

---

## 2. Вопросы G1–G10, требующие ответа DBA

| ID | Вопрос к DBA (кратко) | Без ответа Create? |
| --- | --- | --- |
| **G1** | A или B? Точные SID / PDB / **service** / port? | **Блокер** |
| **G2** | Хост (кандидат `TC-ASUVP-03` или иной): CPU/RAM/disk/IOPS достаточны для TEST? | **Блокер** |
| **G3** | Charset: `AL32UTF8` или `CL8MSWIN1251` + NLS? | **Блокер** |
| **G4** | Partitioning licensed? → будущий **V0** или **V0b** (apply не сейчас) | **Блокер** для выбора пути DDL; не для пустого Create per se, но нужен до Flyway |
| **G5** | Согласованные tablespace / FRA / RMAN retention (дней)? | **Блокер** |
| **G6** | Naming TEST vs Production (service + учётки)? | **Блокер** |
| **G7** | Подтверждение модели `RAIL_APP` + имя migrator; где secret store? (без паролей в ответе) | **Блокер** (дизайн) |
| **G8** | Принимаете план проверки V1–V6 (48 §9.3)? Подпись | **Блокер** |
| **G9** | *(Owner, не DBA)* строка APPROVALS Create TEST | **DENIED** до решения владельца **после** G1–G8 |
| **G10** | Как откатываете пустой TEST (drop PDB/SID)? | **Блокер** |

---

## 3. Матрица ожидания ответов и доказательств

| ID | Ожидаемый артефакт от DBA | Допустимое доказательство | Статус ожидания | Не подставлять |
| --- | --- | --- | --- | --- |
| G1 | Заполненная строка формы 52 + имена | Текст A/B + service из `V$SERVICES` / listener config (redacted) | **WAITING** | Примеры `RAILDB.WORLD` как факт |
| G2 | Числа/вердикт «хватает / не хватает» | Внутренний capacity note; без секретов | **WAITING** | Оценки 300/500 ГиБ из docs 24 как «замер» |
| G3 | Выбранный charset | Решение + краткий NLS note | **WAITING** | «Предпочитаем AL32UTF8» = уже выбрано |
| G4 | YES/NO Partitioning + V0\|V0b | License inventory / DBA statement | **WAITING** | «Наверное V0b» |
| G5 | Цифры tablespace/FRA + retention N дней | Письменное согласование | **WAITING** | Ориентиры 24 §4 как факт хоста |
| G6 | Naming convention | Таблица TEST vs PROD identifiers | **WAITING** | Общие учётки |
| G7 | Имя migrator + путь secret store | Без паролей | **WAITING** | Реальные пароли / wallet в Git |
| G8 | «План V1–V6 принимаю» + ФИО/дата | Подпись в форме 52 | **WAITING** | Молчаливое согласие |
| G9 | ID строки APPROVALS Create TEST | Журнал Owner | **WAITING / DENIED** | Считать P4-2 = Create |
| G10 | Процедура rollback | Шаги drop / кто исполняет | **WAITING** | «Потом решим» |

**Правило закрытия строки:** статус → CONFIRMED только после явного ответа DBA (или Owner для G9). Иначе остаётся WAITING / UNKNOWN / DENIED.

---

## 4. Фиксация запретов (не менять без новых фактов)

| Item | Status |
| --- | --- |
| Create Oracle RAILDB (TEST/Prod) | **DENIED** |
| Flyway / JDBC / DBTC read / RAILDB write / live migrate / FTP·RWL | **DENIED** |
| NK1 | **BLOCKED** |
| DT1 | **BLOCKED** |
| G31 | **OPEN** |
| Watched / M8 populate | **empty / SKIP** until G31 |

---

## 5. Следующий шаг процесса (без CODE)

```text
Передать комплект §1 DBA
  → Дождаться матрицы §3 (WAITING → ответы)
  → Owner: Go/No-Go по doc 50
  → При GO: отдельная строка APPROVALS «Create empty RAILDB TEST»
  → Только затем DBA Create (вне Cursor)
```

Новый CODE-срез / auto-merge / изменение APPROVALS Create — **не** этим документом.
