# 48. DBA Readiness Report (offline) — после Accept ADR-0009 direction

Дата: **2026-11-20**. Источники: [ADR-0009](decisions/ADR-0009-raildb-primary-store.md), [24](24_RAILDB_DBA_INSTRUCTION.md), checklist [43](43_ADR0009_DBA_CREATE_CHECKLIST.md) (если ещё не в `main` — см. PR #8), [42](42_ADR0009_DECISION_READINESS.md), [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md).

**Метод:** offline docs only. **Нет** подключений к Oracle, DDL, Flyway, JDBC, миграции данных.  
**NK1 / DT1 / G31 / R1-Q-TZ** не закрываются этим отчётом.

---

## 1. Контекст разрешений

| Решение | Статус (ожидаемый / факт) |
| --- | --- |
| Accept ADR-0009 direction | Владелец утвердил 2026-11-20; ADR-файл на `main` помечен ACCEPTED; **полная** строка Accept + checklist 43 — в **PR #8** (если OPEN — журнал на `main` ещё может показывать Accept DENIED) |
| Create RAILDB | **DENIED** |
| Flyway / JDBC TEST / migrate / Production | **DENIED** |
| P4-1 offline A/B/C CODE | **APPROVED** (PR #10) — не требует Create |

---

## 2. Предпочтения владельца → вопросы DBA

| Тема | Предпочтение владельца | Что подтвердить DBA | Статус готовности |
| --- | --- | --- | --- |
| Топология | **A** (отдельный instance) | A vs **B** (PDB); SID/PDB; **точный service name**; listener | **NOT READY** — нет DBA confirm |
| Хост | кандидат `TC-ASUVP-03` | CPU/RAM/диск/IOPS/свободное место | **NOT READY** |
| Charset | **AL32UTF8** | Confirm; совместимость с DBTC `CL8MSWIN1251` | **NOT READY** |
| Partitioning | V0 если licensed, иначе V0b | License **да/нет** → DDL path | **NOT READY** |
| Ёмкость | ≥~300 GiB data, FRA ≥~500 GiB (ориентир 24) | Согласовать usable + autoextend | **NOT READY** |
| Backup | ARCHIVELOG + RMAN | Политика + план test restore до cutover | **NOT READY** |
| Учётки | `RAIL_APP` runtime; migrator отдельно | Имена, least privilege, secret store | **NOT READY** |
| Сосуществование | не трогать `RAC.WORLD` / `DBTC` | Подтвердить изоляцию | **NOT READY** |

**Вердикт Create:** **NOT READY** — ни один пункт чеклиста 43 §2 не отмечен как выполненный в репозитории; отдельная строка Create отсутствует.

---

## 3. Чеклист 43 — сводка (все unchecked без DBA)

### До запроса Create

- [ ] Ресурсы хоста  
- [ ] Лицензия instance/PDB  
- [ ] Partitioning → V0/V0b  
- [ ] A или B + service name  
- [ ] Charset/NLS  
- [ ] Tablespace + FRA  
- [ ] Карантин ОС ≥~100 GiB  
- [ ] ARCHIVELOG + RMAN policy  
- [ ] APPROVED «Create Oracle RAILDB»  

### После Create (ещё не применимо)

JDBC URL без пароля; A/B names; charset; пустая `RAIL_APP`; `DBTC` не менялась.

---

## 4. Открытые семантики (не для Create, но для DBA handoff)

| ID | Статус | Нужно от DBA / бизнеса |
| --- | --- | --- |
| **NK1** | BLOCKED | Tape access plan + hour dup probes |
| **DT1** | BLOCKED | То же; ось GETTING hold |
| **G31** | OPEN | Package SPEC / export Active; business pick |
| **R1-Q-TZ** | OPEN | Не блокер Create; политика суток позже |

---

## 5. Что разработка может делать без DBA Create

- Offline synthetic R1–R3, P4-0 stubs  
- **P4-1 A/B/C** (APPROVED) — scale fixtures, FE UX, SelectionEngine L0  
- Docs / DBA checklist заполнение **человеком/DBA**

## 6. Что блокировано до Create + последующих APPROVALS

Create instance · Flyway V0/V0b · JDBC TEST RO/write · DBTC migrate read · RAILDB write · Production · live FTP/RWL · cutover

---

## 7. Рекомендуемые следующие шаги (человек)

1. **Merge PR #8** (если ещё OPEN) — журнал Accept + файл 43 на `main`.  
2. DBA заполняет 43 → владелец отдельным чатом APPROVED Create.  
3. Параллельно: review/merge P4-1 CODE PRs (#11/#12 + slice A).  
4. Не закрывать NK1/DT1/G31/R1-Q-TZ без evidence.

---

## 8. Stop

Этот отчёт **не** разрешает Create и **не** является заполненным DBA checklist.  
Cursor не подключается к Oracle.
