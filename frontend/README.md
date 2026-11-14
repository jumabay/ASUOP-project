# Frontend (R1)

Минимальный UI для A2 (карточка вагона) и A1 (история дислокации за сутки).

## Стек

React + TypeScript + Vite, Ant Design, AG Grid Community.

## Локальный запуск

1. Поднять backend: из корня репозитория (на Windows с кириллическим путём удобнее `subst Z: <repo>` и работать из `Z:\`):

```bat
gradlew.bat :platform-app:bootRun
```

2. В каталоге `frontend`:

```bat
npm install
npm run dev
```

Открыть http://localhost:5173 — Vite проксирует `/api` на `http://localhost:8080`.

Синтетический вагон: `SYN-00001`, дата с данными: `2024-01-15`, пустой день: `2024-01-16`.

## Тесты

```bat
npm test
npm run build
```
