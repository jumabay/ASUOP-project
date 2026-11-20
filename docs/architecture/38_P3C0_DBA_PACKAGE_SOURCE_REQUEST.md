# 38. P3C — запрос DBA: спецификации пакетов + план ленты (без EXECUTE)

Статус после P3C-1: G31 package text для `DBTC_READONLY` по-прежнему **пуст** (0 rows). Запрос актуален.  
Дополнительно для NK1/DT1: нужен **письменный access plan** на 1-hour range `STATEREPORTRECORDS` по `IDX_REC_GETTING_DATE` (без FULL SCAN); EXPLAIN PLAN — только DBA.

Цель: закрыть **G31** и уточнить writers импорта/ремонта **без** выполнения процедур и без побочных эффектов.

## Запрашиваемые объекты (read-only)

| Object | Need | Why |
| --- | --- | --- |
| `DBTC.WAGONPACKAGE` SPEC (+ BODY if policy allows) | текст / `ALL_SOURCE` | `GetActiveWagonList` vs `GetWagonList` |
| `DBTC.IMPORTPKG` SPEC (SaveStateReport* signatures) | текст | KIT_FK / line write semantics clues |
| `DBTC.REPAIRSPKG` SPEC fragments `Register`/`UnRegister` | текст | RP1 open/current |
| `DBTC.WAREHOUSEPKG` SPEC `Register*` names only | список | WH1 Register mapping |

## Способ передачи (предпочтительно)

1. Grant `SELECT` on `ALL_SOURCE` / `ALL_ARGUMENTS` for listed packages to `DBTC_READONLY`, **или**
2. DBA exports SPEC to secured file share (no passwords in export), analyst copies into `docs/analysis/_oracle_tmp/` (gitignored).

## Запрещено этим запросом

- `EXECUTE` / `BEGIN … GetActiveWagonList`
- Выгрузка курсора Active (отдельный approve P3C-1+/B4)
- Изменение пакетов, ролей ANY, DBA role

## Ожидаемый результат для G31

- Подтверждение существования `GetActiveWagonList`
- Сигнатура (OUT cursor)
- Если BODY доступен: предикат membership (таблица/view/флаг) — иначе остаётся UNKNOWN
