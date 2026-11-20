# dbtc-dictionary-inspector (P3B)

Standalone Java 21 tool: sequential audited dictionary SELECTs against Oracle RAC (`RAC.WORLD`), CSV export for offline analysis.

## Safety

- Credentials only from environment (`P3B_ORACLE_*` / `ORACLE_*`). No secret-file hunting.
- Forbidden users: `RAIL_APP`, `DBTC`, `SYS`, `SYSTEM`.
- JDBC `readOnly=true` + connect/query timeouts.
- Stops if `DB_NAME` ≠ `RAC` or `SERVICE_NAME` ≠ `RAC.WORLD`.
- Stops on write/admin session privileges.
- No DDL/DML/PL/SQL/ALTER SESSION.

## Run

```powershell
$env:ORACLE_JDBC_URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD"
$env:ORACLE_USER = "<DBA-approved-read-only>"
$env:ORACLE_PASSWORD = "<from-secure-store>"

cd tools/dbtc-dictionary-inspector
.\gradlew.bat test
.\gradlew.bat run          # P3B dictionary G0–G5
.\gradlew.bat runP3c       # P3C-1: G0 + G31/RP1/WH1/KIT_FK (skips NK1/DT1/EXPLAIN)
```

Results: `docs/architecture/sql/dba/p3b_results/` and `p3c_results/` (gitignored).  
Require Java 21 (`JAVA_HOME` / `-Dorg.gradle.java.home`).

Driver: `ojdbc10:19.33.0.0` + `orai18n:19.33.0.0` (same as `oracle-jdbc-smoke`).
