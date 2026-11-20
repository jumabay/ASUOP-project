# 51. Техническое обращение DBA — пустая Oracle RAILDB TEST

Дата: **2026-11-20**. Автор пакета: ASUOP Manager (docs-only).  
База: `main` @ merge PR #16 (`4de57ff`).  
Источники: [43](43_ADR0009_DBA_CREATE_CHECKLIST.md), [48](48_DBA_HANDOFF_PACKAGE.md), [49](49_NK1_DT1_G31_OFFLINE_RESEARCH.md), [50](50_P4_2_GO_NOGO_CREATE_RAILDB_TEST.md), [24](24_RAILDB_DBA_INSTRUCTION.md).

> **Важно:** этот документ — **запрос к DBA**, а не подтверждение.  
> Заполненный checklist 43 **не** равен подписи DBA и **не** открывает Create.  
> Create возможен только после ответов DBA + **отдельной** строки APPROVALS «Create Oracle RAILDB (TEST)».

**Запрещено исполнителю разработки/агентам по этому обращению:** подключение к Oracle, DDL/Flyway, JDBC, создание instance/PDB, реальные пароли в Git/чате, миграция данных, смена статусов NK1/DT1/G31/R1-Q-TZ.

---

## 1. Единое обращение (текст для передачи DBA)

**Тема:** Подготовка к созданию **пустой** тестовой БД `RAILDB` (TEST) для платформы АСУОП — запрос технических подтверждений.

Уважаемые коллеги DBA,

проект АСУОП (новая Java-платформа) планирует в перспективе отдельное хранилище **`RAILDB`** со схемой **`RAIL_APP`**. Наследие **`DBTC` / service `RAC.WORLD`** на `tc-asuvp-03` **не изменять**.

Сейчас **не** просим создавать БД. Просим **письменно подтвердить или уточнить** параметры ниже (G1–G10), чтобы владелец проекта мог принять решение о **отдельном** разрешении «Create empty RAILDB TEST».

### Что нужно от DBA (ответ по пунктам)

1. **Топология:** вариант **A** (отдельный instance/SID) или **B** (PDB в CDB). Зафиксировать точные имена SID/PDB/**service** (не примеры). Listener **не** должен подменять `RAC.WORLD`.
2. **Хост-кандидат** `TC-ASUVP-03` / `tc-asuvp-03`: подтвердить пригодность для TEST (CPU, RAM, свободный диск, IOPS) или указать другой хост.
3. **Charset:** предпочитаем **`AL32UTF8`**; альтернатива `CL8MSWIN1251` только при явном требовании совместимости с DBTC. Нужно ваше решение + NLS-заметки.
4. **Лицензия Oracle Partitioning** на целевом окружении: **да/нет**. От этого зависит будущий DDL **V0** (partition) vs **V0b** (heap) — apply Flyway **не** в этом шаге.
5. **Ёмкость TEST (ориентиры проекта, не замеры):** tablespace ≥ ~300 ГиБ (лучше 500), FRA ≥ ~500 ГиБ, диск карантина ОС ≥ ~100 ГиБ, RAM-гипотеза 16–32 ГиБ. Подтвердите, что на выбранном хосте это реализуемо для TEST, или дайте свои цифры.
6. **RMAN:** ARCHIVELOG с первого создания; политика incremental/full + retention (дней); подтверждение, что RMAN `DBTC`/RAC не отменяется.
7. **Изоляция TEST ≠ Production:** отдельные service/учётки; naming convention.
8. **Учётки (только дизайн сейчас):** `RAIL_APP` (runtime, least privilege); migrator `RAIL_MIGRATE` (имя TBD). **Не** создавать пароли в ответном письме в открытом виде — указать, куда положите секреты.
9. **План отката пустого TEST:** как снимаете PDB/SID при отказе от эксперимента.
10. **Проверка после будущего Create (когда будет APPROVED):** service resolve, `NLS_CHARACTERSET`, пустая `RAIL_APP`, `DBTC` не тронут, ARCHIVELOG on.

### Что **не** входит в Create empty TEST

- Flyway / бизнес-DDL, JDBC приложения, загрузка истории 12м, заполнение «слежения» (G31 OPEN).
- DDL/DML в `DBTC`, использование RAC-схемы `RAIL_APP` (квота 5 ГиБ) как целевого store.

### Приложения в репозитории

| Документ | Содержание |
| --- | --- |
| [43](43_ADR0009_DBA_CREATE_CHECKLIST.md) | Чеклист Owner/DBA/Status (заполнить **вами**) |
| [48](48_DBA_HANDOFF_PACKAGE.md) | Полный handoff |
| [50](50_P4_2_GO_NOGO_CREATE_RAILDB_TEST.md) | Go/No-Go G1–G10 |
| [49](49_NK1_DT1_G31_OFFLINE_RESEARCH.md) | Открытые семантики ленты/слежения (**не** блокер пустого Create) |
| [sql/dba/p4-2/](sql/dba/p4-2/) | Черновики SQL **DO NOT EXECUTE** (NK1/DT1/G31 — позже, отдельный approve) |

Просим вернуть заполненную таблицу §2 (столбец «Ответ DBA») и подписанный фрагмент §6 чеклиста 43.

С уважением,  
команда разработки АСУОП / ASUOP Manager

---

## 2. Таблица подтверждений G1–G10

| ID | Требование | Текущее доказательство в репо | Ответственный | Статус сейчас | Недостающие сведения / ответ DBA |
| --- | --- | --- | --- | --- | --- |
| **G1** | Топология A или B; SID/PDB/service записаны | Примеры в 43 §2 / 48 §2; выбор не сделан | DBA (+ Owner) | **OPEN / TBD** | Фактические имена; A vs B |
| **G2** | Ёмкость хоста для TEST подтверждена | Оценки sizing 24/48 §5; хост-кандидат назван | DBA | **UNKNOWN** | CPU/RAM/disk/IOPS/`df` на выбранном хосте |
| **G3** | Charset зафиксирован | Предпочтение AL32UTF8 в 48 §3 | Owner + DBA | **UNKNOWN** | Выбранный charset + NLS |
| **G4** | Partitioning YES/NO → V0/V0b | Черновики V0/V0b в `sql/raildb/`; лицензия неизвестна | License owner + DBA | **UNKNOWN** | Лицензия; выбранный вариант DDL (**apply позже**) |
| **G5** | Tablespace + FRA + RMAN для TEST согласованы | Оценки 300/500 ГиБ; RMAN draft 48 §6 | Owner + DBA | **OPEN / TBD** | Согласованные объёмы и retention (дней) |
| **G6** | TEST ≠ Production задокументированы | Политика 48 §7; naming TBD | Owner + DBA | **OPEN** | Naming service/учёток TEST vs PROD |
| **G7** | Модель учёток least privilege (без паролей в Git) | Дизайн 43 §5 / 48 §8 | DBA | **Design only** | Имя migrator; подтверждение модели; secret store path |
| **G8** | План проверки пустого Create согласован | Чеклист V1–V6 в 48 §9.3 | DBA + Eng | **Agreed as plan** | Подпись DBA «принимаю план проверки» |
| **G9** | APPROVALS: Create Oracle RAILDB **TEST** = APPROVED | Строка Create = **DENIED** | Owner | **DENIED** | Новая строка журнала **после** G1–G8 |
| **G10** | Rollback пустого TEST задокументирован | Требование в 50 G10; детали TBD | DBA | **UNKNOWN** | Процедура drop PDB/SID / сроки |

**Итог по таблице:** ни один из G1–G10 **не** закрыт подписью DBA. Документы P4-2 — подготовка, не подтверждение.

---

## 3. Подтверждённые факты vs неизвестные параметры

### 3.1 Подтверждено проектом (не измерение хоста)

| Факт | Основание |
| --- | --- |
| Целевое направление: отдельный store `RAILDB` + схема `RAIL_APP`; `DBTC` = migrate+archive | ADR-0009 / APPROVALS direction docs |
| Нельзя использовать RAC `RAIL_APP` 5 ГиБ как целевой store; не трогать `RAC.WORLD` | 43 §1.14 — **project rule** |
| Create / Flyway / JDBC / migrate / Production access | **DENIED** в APPROVALS |
| P4-2 docs pack на `main` (PR #16) | merge `4de57ff` |
| NK1/DT1 **BLOCKED**, G31 **OPEN**; watched empty until G31 | doc 49 |
| RP1/WH1/KIT_FK (частично) закрыты прошлыми RO-прогонами | doc 39 — **не** относится к Create empty |
| Пустой Create **не** требует закрытия NK1/DT1/G31; migrate/watched — требуют | doc 50 §5 |

### 3.2 Неизвестно / TBD (ждёт DBA или Owner)

| Параметр |
| --- |
| A vs B; точные SID/PDB/service/port |
| Фактическая пригодность `TC-ASUVP-03` (или другой хост) |
| Charset финальный |
| Partitioning licensed? |
| Реальные объёмы disk/FRA/quarantine path |
| RMAN retention (дней) |
| Naming TEST/PROD |
| Имя и grants `RAIL_MIGRATE` |
| Процедура отката пустого TEST |
| Строка APPROVALS Create TEST (ещё не выдана) |

---

## 4. Диагностические SQL (только для DBA; **не исполнять** агентами)

Для **пустого Create** достаточно ответов G1–G10; SQL на `DBTC` **не обязателен**.

Опционально, **после отдельного APPROVED** и только руками DBA:

| Когда | Артефакт | Назначение |
| --- | --- | --- |
| До Create (опционально) | Локальные проверки хоста/лицензии DBA tooling | Не в репозитории |
| После Create (проверка) | Простые SELECT charset/service/`RAIL_APP` — пишет DBA | Не коммитить секреты |
| Позже (семантика) | [sql/dba/p4-2/](sql/dba/p4-2/) NK1/DT1/G31 | **DO NOT EXECUTE** без scoped APPROVED + tape plan |

Шаблон ответа DBA после будущего Create (без паролей):

```text
Topology: A|B
Host:
Port:
SID/PDB:
Service:
Charset:
NLS notes:
RAIL_APP created: Y/N (empty)
ARCHIVELOG: Y/N
DBTC/RAC.WORLD unchanged: Y/N
JDBC URL shape (no password): jdbc:oracle:thin:@//...
```

---

## 5. Go / No-Go для владельца проекта (сейчас)

| Вердикт | Обоснование |
| --- | --- |
| **NO-GO Create RAILDB TEST** | G1–G8/G10 без ответа DBA; G9 = DENIED; checklist ≠ confirmation |
| **GO к следующему шагу процесса** | Передать §1–§2 DBA; ждать письменный ответ; затем рассмотреть строку APPROVALS Create TEST |

### Когда станет возможен GO на Create

Все G1–G8 и G10 заполнены DBA/Owner **без** выдачи UNKNOWN за CONFIRMED → Owner добавляет APPROVALS Create TEST (G9) → DBA исполняет пустой Create по 48 §9.2.

### No-Go триггеры (сохраняются)

N1–N7 из [50](50_P4_2_GO_NOGO_CREATE_RAILDB_TEST.md) (в т.ч. Create на prod RAC, Flyway в том же шаге, пароли в Git, закрытие NK1/DT1/G31 без фактов).
