# P5-E RO smoke — evidence

Дата: **2026-11-24**.  
APPROVALS: P5-E + Production scoped RO override **APPROVED** (time-boxed → `2026-11-25 18:00 +05:00`).  
Prep: [ASUOP_P5_E_TEST_RO_SMOKE_PREP.md](ASUOP_P5_E_TEST_RO_SMOKE_PREP.md) §7.  
Runner: `tools/p5e-ro-smoke` (SELECT-only; password never logged).

## 1. Identity

| Check | Result |
| --- | --- |
| Connect attempted | **NO** |
| `ASUOP_ORACLE_PASSWORD` env | **unset** (Process/User/Machine) |
| Verdict | **STOP-ID** — no connect without approved password env |

Output (redacted):

```text
=== P5-E RO smoke (SELECT-only) ===
URL(redacted)=jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD
user_arg=Dbtc
password_env_set=false
control=54787361 day=2026-11-24
SECTION=identity
STOP-ID: ASUOP_ORACLE_PASSWORD is unset (no connect)
```

## 2. Metadata

**Not run** (blocked by identity STOP).

## 3. Business (A2/A1)

**Not run** (blocked by identity STOP).

Planned on retry after password env is set locally (not in chat/git):

- A2: `WAGON.CODE`/`ID` for `54787361`; other passport fields `SKIPPED_UNKNOWN_MAPPING`
- A1: `GETTING_DATE` half-open `2026-11-24`; max 501; station/operation `SKIPPED_UNKNOWN_MAPPING` until FK unlock

## Next step (operator)

In the same machine session used by the agent (User or Process env), set **only**:

```powershell
$env:ASUOP_ORACLE_PASSWORD = '<from secure store>'
```

Do **not** paste the password into chat, docs, or git. Then re-run smoke; no new APPROVALS needed within the window.
