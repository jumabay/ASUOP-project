# JDK 21 + Windows build/test (P0-2)

Approved runtime: **Java 21** (not PATH default 25). Toolchain in `build.gradle.kts` already requests 21.

## Two local pitfalls

1. **Daemon JDK** — if `~/.gradle/gradle.properties` contains `org.gradle.java.home=.../jdk-17`, Gradle daemon stays on 17 even when `JAVA_HOME` is 21. Project `gradle.properties` **cannot** override that (user home wins). Do not change the global user file unless the human asks; use the script below instead.
2. **Cyrillic/non-ASCII project path** — `gradlew test` from e.g. `...\Project\АСУВП` fails with `ClassNotFoundException` on test classes (e.g. `platform.disl.R0AcceptanceTest`) and sometimes on `GradleWorkerMain`. Workaround: `subst` to an ASCII drive letter.

## Recommended command (agents)

From repo root (PowerShell):

```powershell
.\scripts\gradle-jdk21.ps1 test
.\scripts\gradle-jdk21.ps1 clean test --no-build-cache
.\scripts\gradle-jdk21.ps1 -version
```

Optional env: `ASUOP_JAVA_HOME` (default `C:\Program Files\Java\jdk-21.0.12`).

The script:

- sets `JAVA_HOME` to JDK 21;
- writes an ASCII `GRADLE_USER_HOME` (default `C:\Temp\asuop-gradle-home`) with `org.gradle.java.home` → daemon JVM 21;
- if the repo path is non-ASCII, `subst Z:` (override with `-DriveLetter`) and runs from that drive;
- pass `-NoSubst` only when the checkout path is already ASCII.

Manual equivalent:

```bat
subst Z: C:\Users\...\Project\АСУВП
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.12
set GRADLE_USER_HOME=C:\Temp\asuop-gradle-home
cd /d Z:\
gradlew.bat test
```

## Synthetic boot / REST smoke (P0-5)

See [SYNTHETIC_RUNTIME_SMOKE.md](SYNTHETIC_RUNTIME_SMOKE.md).

```powershell
.\scripts\gradle-jdk21.ps1 :platform-app:bootRun
.\scripts\gradle-jdk21.ps1 :platform-app:test --tests "platform.app.api.SyntheticRuntimeSmokeTest"
.\scripts\rest-smoke-synthetic.ps1   # only if bootRun is already up on :8080
```

## CI note

Prefer an ASCII checkout path (GitHub Actions runners are ASCII). Pin `actions/setup-java` to Temurin **21**. No Production/TEST Oracle in CI without APPROVALS. Workflow `.github/workflows/gradle-jdk21-test.yml` runs full `test` plus an explicit `:platform-app` synthetic REST smoke step.

## Out of scope

No business-logic changes, no Oracle/JDBC enablement, no Flyway apply, no live FTP/RWL.
