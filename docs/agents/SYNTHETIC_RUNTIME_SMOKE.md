# Synthetic runtime smoke (P0-5)

Offline smoke for `platform-app` on profile **`synthetic`**. No Oracle, JDBC, FTP, or RWL.

## Prerequisites

- JDK **21** only (approved path: `C:\Program Files\Java\jdk-21.0.12`). Never use JDK 25 for this project.
- On Windows with a Cyrillic/non-ASCII repo path, use `scripts/gradle-jdk21.ps1` (subst + daemon JDK pin). See [BUILD_JDK21_WINDOWS.md](BUILD_JDK21_WINDOWS.md).

## Boot (manual)

From repo root:

```powershell
.\scripts\gradle-jdk21.ps1 :platform-app:bootRun
```

Default profile is `synthetic` (`application.yml`). Server listens on **http://localhost:8080**.

Expect log lines roughly:

- `The following 1 profile is active: "synthetic"`
- `Tomcat started on port 8080`
- `Started RailwayPlatformApplication`

## REST checks (R1–R3)

| Slice | Method | Path |
| --- | --- | --- |
| Card | GET | `/api/v1/wagons/SYN-00001` |
| Day history (data) | GET | `/api/v1/wagons/SYN-00001/state-reports?date=2024-01-15` |
| Day history (empty) | GET | `/api/v1/wagons/SYN-00001/state-reports?date=2024-01-16` |
| Registry | GET | `/api/v1/wagons` |
| Watched | GET | `/api/v1/wagons/watched` |

Fixture codes: wagon **`SYN-00001`**; day with lines **`2024-01-15`**; empty day **`2024-01-16`**.

Against a running boot:

```powershell
.\scripts\rest-smoke-synthetic.ps1
```

## Automated (preferred for CI)

In-process MockMvc smoke (no listening port, no Oracle):

```powershell
.\scripts\gradle-jdk21.ps1 :platform-app:test --tests "platform.app.api.SyntheticRuntimeSmokeTest"
```

Full suite (includes the smoke class + broader `SyntheticWagonApiIntegrationTest`):

```powershell
.\scripts\gradle-jdk21.ps1 test
```

`JdbcParityIT` stays `@Disabled` until a separate APPROVALS row for TEST Oracle.

## Frontend hand-off

| Item | Value |
| --- | --- |
| API base | `http://localhost:8080` |
| Vite UI | `http://localhost:5173` |
| Preview UI | `http://127.0.0.1:4173` (Playwright default) |
| Proxy | `/api` → `http://localhost:8080` (`frontend/vite.config.ts`, both `server` and `preview`) |
| Smoke wagon | `SYN-00001` |
| Day with data / empty | `2024-01-15` / `2024-01-16` |

### Alternate API port (when 8080 is occupied)

Do **not** kill unknown processes on 8080. Boot backend on another port and point the Vite proxy:

```powershell
# Terminal A — JDK 21 only (SERVER_PORT or --server.port)
$env:SERVER_PORT = "8081"
.\scripts\gradle-jdk21.ps1 :platform-app:bootRun
# alternative: .\scripts\gradle-jdk21.ps1 :platform-app:bootRun --args="--server.port=8081"

# Terminal B — UI / Playwright
cd frontend
$env:API_PROXY_TARGET = "http://localhost:8081"
npm run build
npx playwright test
```

`API_PROXY_TARGET` is read when Vite starts (dev or preview). Default remains `http://localhost:8080`.

### Playwright browser smoke (P1-4)

Synthetic/offline only. Screenshots use fixture codes (`SYN-00001`, dates above) — no secrets/PII.

```powershell
cd frontend
npm ci
npm run test:e2e:install   # local Chromium; corporate TLS may need NODE_TLS_REJECT_UNAUTHORIZED=0 once
npm test                   # Vitest
npm run build
# backend must already answer R1–R3 (see Boot / REST checks)
npm run test:e2e           # starts vite preview, runs e2e/synthetic-smoke.spec.ts
```

CI: job `playwright-smoke` in `.github/workflows/gradle-jdk21-test.yml` boots `:platform-app:bootRun`, waits for `/api/v1/wagons/SYN-00001`, builds the UI, runs Playwright Chromium.

Artifacts (local, gitignored): `frontend/e2e/screenshots/*.png`, `frontend/playwright-report/`, `frontend/test-results/`.

## Out of scope

Production/TEST Oracle, live JDBC, enabling `JdbcParityIT`, FTP/RWL live, schema migrations, changing R1–R3 business contracts.
