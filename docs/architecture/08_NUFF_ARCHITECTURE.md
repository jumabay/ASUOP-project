# 08. НУВ — номерной учёт вагонов

НУВ в исследовании — `WagonApp` 2.2.1.25 и `WagonPlanner` 1.2.2.85. Клиент сам FTP-срез не забирает. Ремонтные формы есть и в НУВ: граница с УВП проводится по модулю `repairs`, не по имени папки. Отличия сборок 2.2.1.25 и 2.2.1.26 не доказаны (G11).

Источник: [04-nuv-reverse.md](../analysis/04-nuv-reverse.md), карточки в [_archive_csharp/MODULES.md](_archive_csharp/MODULES.md).

## Что делает старая программа

Показывает дислокацию и историю срезов, ведёт карточки вагонов, подачу, уборку и планирование. Чтение среза: `GivingDeparture.GetWagonStateReports` → `StateReports.LoadWagonStateReports` → `StateReportPackage.GetWagonStateReports`. Список обозревателя («Список вагонов» / caption «Все вагоны»): `WagonLib.WagonCodeCollection` → `WagonPackage.GetWagonList`. Карточка: `GetWagonByCode` / `LoadWagon`. `GetActiveWagonList` есть в Remoting (`IGivingDepartureCall.GetActiveWagons` → `GivingDepartureLoader`), UI call-site в поставке НУВ **не найден** ([11-nuff-active-wagon-list.md](../analysis/11-nuff-active-wagon-list.md)). Подход: `ApproachPkg.GetApproachWagons` — имя вызова есть, экран по кнопкам не разобран.

Кнопки `WagonPlanner` (наряды, подача, уборка, маршруты, заявки) подтверждены списком классов и именами пакетов `GivingPkg`, `GivingDeparturePackage`, `WorkListsPkg`, не каждой кнопкой (G10). В проектирование записи модуль `movement` не берётся.

## Как это ляжет на модули

| Функция задания | Модуль | Первая реализация | Что остаётся в Oracle |
| --- | --- | --- | --- |
| Реестр вагонов, поиск по номеру | `fleet` | `GetWagonList`, `GetWagonByCode` | пакет чтения |
| Карточка вагона | `fleet` | поля паспорта A2 | `SaveWagon` у старого клиента |
| Текущая дислокация | `fleet` | поля `LOCATION*` как «карточка Oracle», вне сверки со срезом | писатель не доказан (G3, G4) |
| История дислокации | `dislocation` | `GetWagonStateReports` за сутки | пакет и лента |
| Подача, уборка, прибытие, списки слежения | `movement` | чтение после разделения `Get*` и `Save*` | запись у планировщика и импортёра |
| R3 «Вагоны на слежении» (synthetic v1) | `fleet` | READ-ONLY `GET /api/v1/wagons/watched`; membership — fixture `watched.json` (коды ⊂ catalog R2); паспорт A2 из synthetic card | соответствие `GetActiveWagonList` **не подтверждено** (R4: UI НУВ Active не вызывает; обозреватель = `GetWagonList`; `wagons.txt` — другой путь). Публикация FTP / запись — вне R3. Evidence: [11-nuff-active-wagon-list.md](../analysis/11-nuff-active-wagon-list.md) |
| Отправка на FTP, приём сторонней компании | `dislocation` / старый сервис | наблюдатель файла, без записи | `ImportPkg`, публикация `wagons.txt` |
| Аналитика | `reports` | по одному готовому отчёту | ночные jobs витрин |

Поля сверки паспорта A2: номер, модель, род, собственник, станция приписки, признак инвентаря. Текущая станция, `CURRENTSTATE_ID` и дата дислокации в тест не входят.

Пробег за месяц читается через `GetMonthDistances`. Считает его job `UPDATEWAGONMONTHDISTANCESDAILY`. Новый код job не вызывает.

## Логика, которую не переносим механически

- Не выбрасывать строку файла из-за отсутствия движения на стороне Java. Эталон сохранения — результат пакета, не догадка (G1).
- Не считать `wagons.txt` почасовым срезом (M3).
- Не переносить Remoting и порты 5502 / 5507.
- Не вызывать `StateReportPackage.DeleteReport` и `WorkListsPkg.UpdateStateReport` в волне чтения.
- Не сверять станцию карточки с последней строкой среза.

## Проверка

| Тест | Что совпадает | Чего тест не открывает |
| --- | --- | --- |
| A1 | число строк и ключи истории за сутки | замену импортёра |
| A2 | поля паспорта | `SaveWagon` и сверку станции |
| B1 | ключи одного файла KZ на двух копиях | промышленный DML и каналы TIS, контейнеров, Excel |

Риски: G1, G2, G3, G4, G9, G10, G11, M2, M3. Пока G10 открыт, экраны подачи в запись не проектируются и в группу A не входят.
