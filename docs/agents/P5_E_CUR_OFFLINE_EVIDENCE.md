# P5-E-CUR — offline synthetic evidence

Дата: **2026-11-24**.  
Scope: **synthetic / offline only**. Does **not** verify Oracle profile `dbtc-ro` or live CONTROL.

PR HEAD: **`f52a5789d22b72dc09b800c218607bce1b4b6b33`**.

Live CONTROL dbtc-ro: [P5_E_CUR_CONTROL_EVIDENCE.md](P5_E_CUR_CONTROL_EVIDENCE.md).

## Suites

| Suite | Channel | Result |
| --- | --- | --- |
| Gradle `test` JDK 21 | Local Independent QA + focused | **PASS** (~179 tests, 0 fail, 1 skipped `JdbcParityIT`) |
| Vitest | Local | **51/51** |
| `npm run build` | Local | **OK** |
| Playwright | Local + **CI** | **19/19** |

## Playwright API process (local QA)

| Item | Value |
| --- | --- |
| Intent | Dedicated synthetic `bootRun` for the QA session |
| Command | `gradle-jdk21.ps1 :platform-app:bootRun --args=--server.port=18080` |
| Process | Java PID **49060**, profile **`synthetic`**, Tomcat **`:18080`** |
| Client | `API_PROXY_TARGET=http://localhost:18080` → `npx playwright test` |
| Not used | Leftover `:8081` / other listeners; **not** `dbtc-ro` |
| Shutdown | Process stopped after e2e (`bootRun` exit −1 = killed) — expected |

## Playwright API process (CI on PR HEAD)

| Item | Value |
| --- | --- |
| Workflow | `.github/workflows/gradle-jdk21-test.yml` job `playwright-smoke` |
| Backend | Job-local `./gradlew :platform-app:bootRun` on **`:8080`** (synthetic default) |
| Client | `API_PROXY_TARGET=http://127.0.0.1:8080` |
| Runs @ `f52a578` | [45369837611](https://github.com/jumabay/ASUOP-project/actions/runs/45369837611), [45369817699](https://github.com/jumabay/ASUOP-project/actions/runs/45369817699) — **success** |

**Verdict:** offline Playwright exercised the **intended synthetic API process** (local `:18080` or CI `:8080`), not a random leftover server, and **not** the Oracle/`dbtc-ro` path.
