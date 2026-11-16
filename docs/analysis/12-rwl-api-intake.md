# 12. RWL API — исследование (RailWagonLocation)

Дата: 2026-11-16. Режим: **документальный анализ**, без live-вызовов API и без записи в Oracle в этом срезе.

## Источники

| Источник | Путь / артефакт |
| --- | --- |
| Официальное описание API (RU) | `Старая программа/RWL-API-Rus.docx` |
| Прототип CLI (не runtime app) | `tools/rwl-import-proto/` |
| Staging DDL прототипа | `tools/rwl-import-proto/src/main/resources/ddl/oracle-staging.sql` |

Секреты и пароли в документ **не** включаются.

## 1. Краткий вывод

RWL API **умеет** отдавать дислокацию и паспортные поля по вагонам на слежении (и историю/ремонты отдельными request_type). Это **кандидат** самостоятельного источника для `RAILDB`.

**Не доказано**, что RWL заменяет FTP hourly full slice (`disl_kz*.txt` ≈ 18k строк/час) по полноте полей, периодичности и лимитам. До B1-сверки FTP остаётся обязательным запасным контуром в архитектуре.

## 2. Транспорт и аутентификация (CONFIRMED doc)

- Base URL: `https://www.railwagonlocation.com/xml/export.php`
- Параметры: `name`, `password`, `request_type`, опционально `return_format`
- `return_format`: `xml` (default), `xmlZipped`, `json`, `jsonZipped`
- Только HTTPS

## 3. Модель данных ответа (CONFIRMED doc)

Корень по вагону:

```text
vagon
  vagon_info   — номер, from/to, даты отправки, группы, флаги слежения, …
  position     — текущая операция/место, utc_datetime, коды станций, поезд, груз, координаты, …
```

Ключевые поля дислокации (фрагмент): `utc_datetime`, `current_position_code`, `current_position`, `operation_code`, `operation_asoup_code`, `operation`, `train_index`, `train_number`, lat/lon.

Proto нормализует подмножество в `RwlOperation` и считает гипотетический `event_key`:

`wagon_no|utc_datetime|station_code|operation_asoup_code|train_index`

плюс `content_hash` для unchanged vs revised. **Это гипотеза прототипа**, не контракт RWL (у API нет явного event id; `operation_id` в XML трактуется как тип операции).

## 4. Запросы, полезные для дислокации / слежения

### 4.1. Несколько вагонов / списки

| request_type | Назначение (по doc) |
| --- | --- |
| `get_user_vagons` | все вагоны пользователя; опции `all_operations`, `group_id`, `added_last_minutes`, `add_vagon_specs`, `add_vagon_repairs` |
| `get_group_vagons` | вагоны группы (`group_login` / `group_password`) |
| `view_vagons` | по списку `client_ids` |
| `search_vagons` | поиск по номерам; флаги `search_in_archive`, `full_history`, `all_operations` |

### 4.2. Один вагон

| request_type | Назначение |
| --- | --- |
| `view_vagon` | вагон на слежении по `vagon_no` |
| `get_one_vagon` / `get_one_vagon_in_group` | варианты с группой |

### 4.3. История

| request_type | Параметры (doc) |
| --- | --- |
| `view_vagon_history` | `vagon_no`, опц. `days_limit`, `calendar_date`, `date_type` |
| `view_vagon_in_group_history` | + group credentials |
| `view_vagon_history_by_id` / `…_in_group_…` | по `client_id` |

### 4.4. Ремонты / sync / слежение

- `view_vagon_repairs`, раздел 6.8 — ремонты
- `sync_vagons`, `sync_vagons_territory` — синхронизация списка
- Установка/снятие слежения — разделы 3–4 doc
- Справки 2730/2731 и др. — отдельные request_type (не FTP-срез)

## 5. Полные vs «инкрементальные» обновления

| Механизм | Оценка |
| --- | --- |
| Полный часовой файл как в ASUVP FTP | В RWL **нет** аналога «один файл = все вагоны часа» в терминах ImportPkg |
| `get_user_vagons` / `view_vagons` | снимок текущих состояний набора вагонов |
| `added_last_minutes` | **кандидат** на узкое окно изменений; семантика «полный час» **не доказана** |
| `all_operations` | влияет на объём операций в ответе; точное поведение — VERIFY на live |
| `view_vagon_history` + `days_limit` | история по одному вагону, не bulk hour kit |

**Вывод:** RWL ближе к «текущее + история по запросу», чем к R0 full-hour kit. Сохранять семантику полных срезов FTP отдельно, пока не доказано иное.

## 6. Ограничения и пробелы vs FTP (UNKNOWN / не доказано)

| Вопрос | Статус |
| --- | --- |
| Rate limit / throttling | **не указан** в извлечённом тексте docx явно → UNKNOWN |
| Max число вагонов в одном запросе | proto искусственно ограничивает live `view_vagon` до **5**; doc допускает списки — лимит провайдера UNKNOWN |
| Покрытие полей KZ 20/21 tab-file | не сведено построчно → нужна матрица B1 |
| Периодичность обновления на стороне RWL | UNKNOWN |
| Эквивалент каналам RU/TIS/containers/Excel | UNKNOWN / частично другие request_type |
| Стоимость запросов / квоты аккаунта | вне doc extract → UNKNOWN |
| Можно ли собрать ~18k вагонов/час стабильно | UNKNOWN |

## 7. Что уже сделано в репозитории (proto)

`tools/rwl-import-proto`:

- Live только `view_vagon` по списку env (max 5).
- `get_user_vagons` **не** вызывается по умолчанию.
- Staging tables: `rwl_import_run`, `rwl_staging_operation`, `rwl_staging_current`, `rwl_staging_reject`.
- Целевой staging user в README: `RAIL_APP` (исторически RAC) — **не** путать с будущим `RAILDB.RAIL_APP` без отдельного approve.
- Не часть `platform-app`; synthetic R0–R3 не затрагивает.

## 8. Как сохранять в RAILDB (дизайн, не реализация)

После появления `RAILDB` и approve write:

1. `RwlApiClient` получает XML/JSON.
2. `RAIL_INTAKE_EVENT` — факт запроса (идемпотентный ключ run).
3. Нормализация → append `RAIL_DISL_EVENT` / update `RAIL_WAGON_CURRENT` в одной TX.
4. Браки → `RAIL_IMPORT_ERROR` / reject.
5. Не вызывать `ImportPkg`, не писать в `DBTC`.

FTP/`FtpIntake` остаётся параллельным адаптером до доказательства замены.

## 9. Продолжение исследования

Полевая матрица и FTP-вывод: [13-rwl-kz-field-matrix.md](13-rwl-kz-field-matrix.md).  
Java offline pipeline: [26_RWL_JAVA_INTEGRATION.md](../architecture/26_RWL_JAVA_INTEGRATION.md), модули `modules/intake`, `platform-rwl`.

Следующие шаги (нужен отдельный approve / не P2):

1. Scoped live: `view_vagon` / `get_user_vagons` на тестовых номерах, redacted fixtures.  
2. Замерить время/объём ответа на N=100/1000/all.  
3. Сравнить один час FTP sample с RWL snapshot (B1).  
4. Только затем решать, может ли RWL быть primary intake.

## 10. Связь с ADR-0009

ADR-0009 (PROPOSED) прямо фиксирует: RWL не считается заменой FTP без подтверждения полноты, лимитов и периодичности. Этот документ — evidence research-gate.
