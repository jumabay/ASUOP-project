# 43. DBA checklist — создание RAILDB (после Accept ADR-0009)

Статус: **чеклист для согласования**. Accept ADR-0009 (2026-11-20) = **architecture direction only**.  
**Не выполнять** Create / DDL / Flyway / выдачу Production JDBC без отдельной строки `Create Oracle RAILDB` = APPROVED в [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md).

Связано: [ADR-0009](decisions/ADR-0009-raildb-primary-store.md), [24_RAILDB_DBA_INSTRUCTION.md](24_RAILDB_DBA_INSTRUCTION.md), [42_ADR0009_DECISION_READINESS.md](42_ADR0009_DECISION_READINESS.md).

---

## 1. Предпочтения владельца (уже зафиксированы)

| Тема | Предпочтение | DBA действие |
| --- | --- | --- |
| Топология | **A** (отдельный instance) предпочтительно | Подтвердить A или обосновать **B** (PDB); зафиксировать SID/PDB/service |
| Charset | **AL32UTF8** предпочтительно | Confirm; оценка совместимости с DBTC `CL8MSWIN1251` |
| DDL | **V0** если Partitioning licensed, иначе **V0b** | Confirm Partitioning license; выбрать path |
| Create сейчас | **Запрещено** до отдельного APPROVED | Заполнить чеклист → запросить APPROVED |
| RAC `RAIL_APP` 5 GiB | Не целевой store | Не использовать как prod RAILDB |

---

## 2. Чеклист до запроса «Create Oracle RAILDB»

### Хост и лицензия

- [ ] Хост-кандидат `TC-ASUVP-03` (или утверждённый альтернативный): CPU / RAM / диск / IOPS / свободное место
- [ ] Лицензия на дополнительный instance или PDB подтверждена
- [ ] **Partitioning option:** licensed **да/нет** → путь **V0 / V0b**
- [ ] Сосуществование с `RAC.WORLD` / `DBTC` без переименования сервиса

### Топология и имена

- [ ] Выбран **A** или **B** (с обоснованием, если не A)
- [ ] Зафиксированы: SID и/или PDB name, **точный service name**, listener
- [ ] JDBC URL шаблон (без пароля): host / port / service

### Charset / NLS

- [ ] Charset instance/PDB зафиксирован (цель AL32UTF8)
- [ ] План NLS при чтении DBTC (клиент `orai18n` уже в ADR-0003)
- [ ] Риски конвертации строковых полей при миграции отмечены

### Ёмкость и backup (ориентиры doc 24)

- [ ] Tablespace данных ≥ **~300 GiB** usable (лучше 500) согласован
- [ ] FRA / backup area ≥ политика retention (ориентир **≥ ~500 ГиБ**)
- [ ] Диск карантина ОС ≥ **~100 ГиБ** (отдельно от tablespace)
- [ ] ARCHIVELOG для `RAILDB`
- [ ] RMAN политика (daily inc + weekly full или эквивалент заказчика)
- [ ] План **test restore** на копии до cutover (не до первого Create обязательно, но до cutover — да)

### Учётки (имена TBD)

- [ ] Runtime `RAIL_APP` (least privilege, без DBA)
- [ ] Migrator учётка (отдельный approve на grants)
- [ ] Секреты только secret store / env — не Git

### Документы / approvals

- [ ] ADR-0009 Accepted (direction) прочитан
- [ ] Rollback policy ≤72h + quarantine replay принята владельцем
- [ ] Строка APPROVALS **«Create Oracle RAILDB» = APPROVED** получена **до** Create
- [ ] Понятно: Create ≠ Flyway apply ≠ JDBC ≠ migrate (каждое — своя строка)

---

## 3. После Create (сообщить разработке, без паролей)

1. Точный JDBC URL (host/port/service).  
2. A/B + SID/PDB/service names.  
3. Charset / NLS.  
4. Пустая схема `RAIL_APP` в `RAILDB` создана (или только instance — уточнить).  
5. Подтверждение: `DBTC` / `RAC.WORLD` не изменялись.  
6. Partitioning: факт → V0 или V0b для следующего Flyway gate.

---

## 4. Явно вне этого чеклиста (отдельные gates)

| Gate | Статус после Accept direction |
| --- | --- |
| Flyway apply V0/V0b | DENIED UNTIL APPROVED |
| TEST JDBC RO | DENIED UNTIL APPROVED |
| RAILDB write / DBTC migrate read | DENIED UNTIL APPROVED |
| NK1 / DT1 tape probes | BLOCKED без DBA access plan |
| G31 / R1-Q-TZ | OPEN — не закрывать Create |
| Production JDBC | DENIED |
| Cutover + rollback drill | После наполнения + отдельный APPROVED |

---

## 5. Stop

Заполнение чеклиста и согласование с владельцем **не** равны Create.  
Cursor / агенты **не** подключаются к Oracle и **не** создают БД по этому документу.
