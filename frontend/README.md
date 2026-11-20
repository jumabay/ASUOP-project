# Frontend (R1)

Минимальный UI для A2 (карточка вагона) и A1 (история дислокации за сутки).

## Стек

React + TypeScript + Vite, Ant Design, AG Grid Community.

## Локальный запуск

1. Поднять backend: из корня репозитория. На Windows с кириллическим путём используйте ASCII `subst` или скрипт (см. `docs/agents/BUILD_JDK21_WINDOWS.md`):

```powershell
.\scripts\gradle-jdk21.ps1 :platform-app:bootRun
```

Эквивалент вручную: `subst Z: <repo>`, `cd /d Z:\`, затем `gradlew.bat :platform-app:bootRun` при `JAVA_HOME` = JDK 21.

2. В каталоге `frontend`:

```bat
npm install
npm run dev
```

Открыть http://localhost:5173 — Vite проксирует `/api` на `http://localhost:8080`.

Синтетический вагон: `SYN-00001`, дата с данными: `2024-01-15`, пустой день: `2024-01-16`.

Backend smoke (JDK 21, profile `synthetic`): `docs/agents/SYNTHETIC_RUNTIME_SMOKE.md`.

## Тесты

```bat
npm test
npm run build
```
