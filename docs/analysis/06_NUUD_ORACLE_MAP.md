# НУД: карта Oracle и стык с УВП

Сессия словаря в этом проходе не открывалась. Имена процедур взяты из spec `docs/analysis/_oracle_tmp/source/PACKAGE__WAREHOUSEPKG.sql` и `PACKAGE__PARTSPACKAGE.sql`, из атрибутов копий `WarehouseLib` / `WagonLib` и из триггеров снимка `pl_triggers.json`. Тела `Register*` / `Save*` скрыты: запросы проводок не выполнялись. Список таблиц у `WarehousePkg` в `ALL_DEPENDENCIES` по-прежнему не виден, поэтому таблица ниже указана там, где её называют триггер, представление, `INSERT` отдельной процедуры или атрибут `[Table]`.

Схема `DBTC`. Клиент доходит до неё так:

`WarehouseApp` → `RemoteDbManager` → `IDbManagerCall` на сервисе приложений → пакет.

Чтение ремонта вагона со строки списания: `RemoteWagon` → `IWagonCall.LoadRepair`.

## 1. Виды документов

Константы `WarehousePkg`:

| Код | Константа | Процедуры шапки | Процедуры строк |
| --- | --- | --- | --- |
| 0 | `DOC_PROCUREMENT` | `SaveProcurementDocument`, `LoadProcurementDocument`, `DeleteProcurementDocument`, `RegisterProcurementDocument`, `UnregisterProcurementDocument` | `DeleteProcurementDetails`, `SaveProcurementDetails`, `LoadProcurementDetails`, `UpdateProcurementDocument` |
| 1 | `DOC_ARRIVAL` | `Save/Load/Delete/Register/UnregisterArrivalDocument` | `Delete/Save/LoadArrivalDetails`, `UpdateArrivalDocument` |
| 2 | `DOC_TRANSFER` | `Save/Load/Delete/Register/UnregisterTransferDocument` | `Delete/Save/LoadTransferDetails`, `UpdateTransferDocument` |
| 3 | `DOC_WRITINGOFF` | `Save/Load/Delete/Register/UnregisterWritingOffDocument` | `Delete/Save/LoadWritingOffDetails`, `UpdateWritingOffDocument` |
| 4 | `DOC_WHEELPAIRREPAIR` | `Save/Load/DeleteWheelPairRepairDocument`, `RegisterWheelPairRepair`, `UnRegisterWheelPairRepair` | `Delete/Save/LoadWheelPairDetails`, `UpdateWheelPairRepair` |
| 5 | `DOC_DETAILSWORKINGSHEET` | `Save/Load/DeleteDetailsWorkingSheet`, `Register/UnRegisterDetailsWorkingSheet` | `SaveDetailsWorkingSheetPart`, `LoadDetailsWorkingSheetParts` |
| 6 | `DOC_REPAIR` | `Save/Load/Delete/Register/UnregisterRepairDocument` | `Delete/Save/LoadRepairDetails`, `UpdateRepairDocument` |

Общий список реестра: `GetDocumentList(begin, end)`. Колонки, которые читает сетка: `ID`, `REGISTER_TYPE`, `DOCTYPENAME`, `WAREHOUSE`, `PARTNER`, `DELIVERYDATE`, `PARTTYPE`, `ROWSCOUNT`, `DESCRIPTION`, `doctype`. Представление под курсором в spec не названо.

Проводка и снятие проводки принимают только `id`. Что именно они меняют в `WAREHOUSEDETAILS`, тело не показывает.

Другие константы пакета, до кнопки не доходящие как подписи формы:

| Группа | Коды |
| --- | --- |
| проводка | `UNREGISTER = 0`, `REGISTER = 1` |
| тип списания | 1 продажа, 2 на вагон, 3 списание, 4 на колёсную пару, 5 на металлолом |
| объект строки | 0 деталь, 1 металл |
| ремонт колёсной пары | 1 смена оси, 2 смена колеса |
| тип склада | 0 основной, 1 ответственное хранение |

## 2. Склад и справочники

| Процедура или атрибут | Кто вызывает | Объект |
| --- | --- | --- |
| `SaveWarehouse(id, name, repairplant_id, warehousetype_id)` | карточка склада | `WAREHOUSES`, список `WAREHOUSESVIEW`. Триггер id `TR_WAREHOUSE_ID` |
| `LoadWarehouses` / `DeleteWarehouse` | справочник | то же |
| `LoadWarehouseDetails(warehouse, detailtype)` | окно «Склад деталей», подбор строк | `WAREHOUSEDETAILS`. Представление `WAREHOUSEOBJECTSVIEW` зависит от `WAREHOUSEDETAILS`, `WAREHOUSES`, `WAGONPARTS`, `PARTTYPES` |
| `[Table] WAREHOUSETYPES` | тип склада, правка с меню закрыта | триггера id в ключевом наборе нет |
| `[Table] WAREHOUSEITEMSVIEW` | фильтр типа на остатке | представление, не таблица остатка |
| `[Table] BUYINGSTATES` | состояние покупки | триггер `TR_BUYINGTYPES_ID` |
| `Directories.SaveBuyingStateRelation` / `LoadBuyingStateRelations` / `DeleteBuyingStatesRelations` | связь состояния покупки с состоянием детали | таблицы связи spec не называет |
| `[Table] DETAILSTATES` | состояние детали | триггер `TR_DETAILSTATES_ID` |
| `[Table] WRITINGOFFTYPES` / `WRITINGOFFREASONS` | тип и причина списания | триггеры `TR_WRITINOFF_TYPE_ID` и `TR_WRITINGOFFREASONS_ID` |
| `[Table] WHEELPAIRREPAIRTYPES` | тип ремонта пары | в меню справочников НУД не зарегистрирован |
| `[Table] DOCUMENTTYPES` / `OBJECTTYPES` | подписи видов | правка закрыта атрибутом |
| `[Table] PARTTYPESVIEW` | тип детали | таблица `PARTTYPES`, триггер `TR_PARTTYPE_ID`. Процедуры `PartsPackage.SaveDetailType` / `LoadDetailType` / `DeleteDetailType` |
| `[Table] STEELDEGREE` | марка металла | поле типа детали |
| `[Table] METALVIEW` | металл | таблица `METAL`, `Directories.SaveMetal` / `LoadMetal` / `DeleteMetal` |
| `[Table] VARIANCEREASONS` | причина несоответствия | строка прихода и ремонта склада |
| `[Table] PARTNERSVIEW` | контрагент покупки и покупатель списания | `CustomerPkg.SavePartner`. В загрузчике покупки контрагент ищется в `CUSTOMERS` с `app_id = 4` |

`FROM WAREHOUSESVIEW` — корень конструктора отчётов НУД (`Applications.WarehouseApp`).

## 3. Экземпляр детали

| Процедура | Аргументы | Куда кладёт клиент |
| --- | --- | --- |
| `SaveDetail` | id, тип | модель 0 |
| `SaveWheelPair` | номер, клеймо, id, тип, год | модель 1 |
| `SaveManufactoringDetail` | id, тип, номер, клеймо, год | модель 2 |
| `SaveHatchCover` | id, тип, номер чертежа | модель 3 |
| `LoadDetail` / `DeleteDetail` | id | карточка |
| `GetDetailId` | id, номер, тип | в клиенте НУД вызова нет |
| `GetLifeTime` | id детали | поле только для чтения у изготовленной детали |
| `SearchParts` | тип, номер, клеймо, год, чертёж | меню «Поиск деталей» |
| `GetHistoryState` | id → number | значок в сетке |
| `LoadPartHistory` | id → курсор `partspackage$history_set` | окно «История детали», колонки `history_date`, `history_text` |

Таблица экземпляра — `WAGONPARTS`, триггер `TR_WAGONPART_ID`. Представления, которые на неё смотрят в снимке зависимостей: `PARTSAFTERVIEW`, `LIFETIMEWAGONPARTSVIEW`, `WAREHOUSEOBJECTSVIEW`, `PROCUREMENTOBJECTSVIEW`, `ARRIVALOBJECTSVIEW`.

Уникальный ключ `(partnumber, type)` в этом снимке не зафиксирован. Единственное видимое правило поиска — в теле `SP_ARRIVALWAGONPARTS`, `SP_ARRIVEWHEELPAIR`, `SP_TRANSFERWAGONPART`: одна строка по номеру и типу либо вставка. Эти процедуры клиент не вызывает.

## 4. Таблицы шапок и строк

Подтверждены триггером, `INSERT` загрузчика или зависимостью представления.

| Таблица | Откуда имя | Документ |
| --- | --- | --- |
| `PROCUREMENT` | `INSERT` в `SP_ARRIVALWAGONPARTS`, триггер `TR_PROCUREMENT_ID` | покупка. Поля вставки: склад, дата, `register_type`, тип детали, контрагент |
| `PROCUREMENTDETAILS` | `INSERT` того же загрузчика | строка покупки: владелец, деталь, количество |
| `ARRIVALDOCUMENTS` | `INSERT` в `SP_ARRIVEWHEELPAIR`, триггер `TR_ARRIVALDOCUMENTS_ID` | приход |
| `ARRIVALDETAILS` | `INSERT` того же загрузчика | строка прихода: владелец, деталь, количество, тип объекта |
| `TRANSFERDOCUMENTS` | `INSERT` в `SP_TRANSFERWAGONPART`, триггер `TR_TRANSFERDOCUMENTS_ID` | перемещение. Поля: склад-получатель, дата, проводка, тип детали, склад-отправитель |
| `TRANSFERDETAILS` | `INSERT` того же загрузчика | строка: владелец, деталь, тип объекта, количество |
| `WRITINGOFFDOCUMENTS` | триггер `TR_WRITINGOFFDOCUMENTS_ID` | списание |
| `WRITINGOFFDETAILS` | зависимость `WRITINGOFFOBJECTSVIEW` и соседних представлений списания | строка списания |
| `WHEELPAIRREPAIRDOCS` | триггер `TR_WHEELPAIRREPAIRDOCS_ID`, представление `WHEELPAIROBJECTSVIEW` | ремонт колёсной пары |
| `WHEELPAIRREPAIRDETAILS` | зависимость `WRITINGOFFWHEELPAIROBJECTSVIEW` | строки ремонта пары |
| `DETAILSWORKINGSHEET` | триггер `TR_DETAILSWORKINGSHEET_ID` | оборотная ведомость |
| `REPAIRDOCUMENTS` | триггер `TR_REPAIRDOCUMENTS_ID` | ремонт склада, не `REPAIRS` |

Имя таблицы строк ведомости и имя таблицы строк ремонта склада по триггеру не подтверждены. Процедуры называются `SaveDetailsWorkingSheetPart` и `SaveRepairDetails`. Представления `ARRIVALOBJECTSVIEW`, `PROCUREMENTOBJECTSVIEW`, `WRITINGOFFOBJECTSVIEW`, `WRITINGOFFSALEOBJECTSVIEW`, `WRITINGOFFTRANSFEROBJECTSVIEW`, `WRITINGOFFWHEELPAIROBJECTSVIEW`, `WHEELPAIROBJECTSVIEW` клиент сеткой напрямую не открывает: реестр идёт через `GetDocumentList`.

`VIRTUALWAREHOUSEDETAILS` есть в снимке этапа 2. Её читает `PartsPackage.LoadVirtualDetails`. Пишет в неё `AddBeforeToVirtualWarehouse(repair_id)`; вызова из `WarehouseApp` нет.

## 5. Стык с УВП

Общие объекты, которые уже вызываются с обеих сторон:

| Объект | НУД | УВП |
| --- | --- | --- |
| `WAGONPARTS` и `PartsPackage.SaveWheelPair` / `SaveManufactoringDetail` / `SaveHatchCover` / `SaveDetail` | карточка детали перед строкой документа | начальные и ремонтные детали вагона |
| `GetHistoryState`, `LoadPartHistory`, `SearchParts` | остаток и поиск | вкладки деталей и «Поиск деталей» |
| `REPAIRPLANTS` | склад привязан к предприятию | предприятие ремонта |
| `REPAIRS` | только чтение по id со строки списания и колонка ведомости | запись ремонта |
| `PARTSAFTER` / `PARTSREMOVED` / `CURRENTPARTS` | кнопками НУД не пишутся | карточка ремонта |
| `WAREHOUSEDETAILS` | проводка склада | УВП `Register` склада не вызывает |
| `REPAIRDOCUMENTS` | документ вида 6 | в меню ремонта вагона нет |

`PartsPackage.WritingOffInstalledDetails`, `ArrivalRemovedDetails`, `ArriveInstalledDetails`, `WriteOffRemovedDetails` относятся к снятым и предустановленным деталям вагона. В `WarehouseApp` их нет.

Сообщение 2731 (`RepairsPkg.ImportWagonParts2731`) пишет номерные позиции вагона. Это контур сервиса УВП, не кнопка склада.

## 6. Что не доказано

- Текст `Register*`: какие строки `WAREHOUSEDETAILS` он вставляет и запрещает ли повторную проводку.
- Есть ли уникальный индекс на `WAGONPARTS(partnumber, type)` или на пару «деталь + склад».
- Имя таблицы строк `SaveRepairDetails` и `SaveDetailsWorkingSheetPart`.
- Входит ли ремонт вагона в `history_text`.
- Вызывает ли сервис `SP_ARRIVALWAGONPARTS` / `SP_ARRIVEWHEELPAIR` / `SP_TRANSFERWAGONPART` по расписанию. В клиенте НУД этих имён нет.
