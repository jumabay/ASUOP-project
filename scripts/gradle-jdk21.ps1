<#
.SYNOPSIS
  Run Gradle wrapper on approved JDK 21, bypassing a user ~/.gradle org.gradle.java.home
  that may pin JDK 17, and optionally mapping a Cyrillic Windows repo path to an ASCII drive.

.DESCRIPTION
  - Sets JAVA_HOME to JDK 21 (ASUOP_JAVA_HOME or default C:\Program Files\Java\jdk-21.0.12).
  - Uses an ASCII GRADLE_USER_HOME (default C:\Temp\asuop-gradle-home) with org.gradle.java.home
    so the daemon actually runs on JDK 21. Project gradle.properties cannot override user home.
  - On Windows, if the repo path is non-ASCII, maps subst drive (default Z:) and runs from there
    to avoid ClassNotFoundException in Gradle test workers.

.EXAMPLE
  .\scripts\gradle-jdk21.ps1 test
  .\scripts\gradle-jdk21.ps1 clean test --no-build-cache
  .\scripts\gradle-jdk21.ps1 -NoSubst -version
#>
# PositionalBinding=$false so "clean test" go to GradleArgs, not DriveLetter.
[CmdletBinding(PositionalBinding = $false)]
param(
    [switch]$NoSubst,
    [string]$DriveLetter = "Z",
    [string]$GradleUserHome = "C:\Temp\asuop-gradle-home",
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"

function Test-AsciiPath([string]$Path) {
    return $Path -match '^[\x00-\x7F]+$'
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = $env:ASUOP_JAVA_HOME
if (-not $javaHome) {
    $javaHome = "C:\Program Files\Java\jdk-21.0.12"
}
if (-not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
    throw "JDK 21 not found at '$javaHome'. Set ASUOP_JAVA_HOME to the approved JDK 21 install."
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;" + $env:PATH

New-Item -ItemType Directory -Force -Path $GradleUserHome | Out-Null
$propsPath = Join-Path $GradleUserHome "gradle.properties"
$javaHomeProp = $javaHome -replace '\\', '/'
@"
org.gradle.java.home=$javaHomeProp
org.gradle.jvmargs=-Dfile.encoding=UTF-8
"@ | Set-Content -Path $propsPath -Encoding ASCII

# Reuse caches/wrapper from the default user home when present (avoid re-download).
$defaultHome = Join-Path $env:USERPROFILE ".gradle"
foreach ($name in @("wrapper", "caches", "native", "notifications")) {
    $src = Join-Path $defaultHome $name
    $dst = Join-Path $GradleUserHome $name
    if ((Test-Path $src) -and -not (Test-Path $dst)) {
        cmd /c "mklink /J `"$dst`" `"$src`"" | Out-Null
    }
}

$env:GRADLE_USER_HOME = $GradleUserHome

$workDir = $repoRoot.Path
$usedSubst = $false
$onWindows = ($env:OS -match "Windows") -or ($PSVersionTable.PSEdition -eq "Desktop")
if (-not $NoSubst -and $onWindows) {
    if (-not (Test-AsciiPath $workDir)) {
        $drive = $DriveLetter.TrimEnd(':')
        cmd /c "subst ${drive}: /D" 2>$null | Out-Null
        cmd /c "subst ${drive}: `"$workDir`"" | Out-Null
        $workDir = "${drive}:\"
        $usedSubst = $true
        Write-Host "Using subst ${drive}: -> $repoRoot (Cyrillic/non-ASCII path workaround)"
    }
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "GRADLE_USER_HOME=$env:GRADLE_USER_HOME"
Write-Host "CWD=$workDir"
if ($usedSubst) { Write-Host "subst=yes" } else { Write-Host "subst=no" }

Push-Location $workDir
try {
    $gradlew = Join-Path $workDir "gradlew.bat"
    if (-not (Test-Path $gradlew)) {
        $gradlew = Join-Path $workDir "gradlew"
    }
    if ($GradleArgs.Count -eq 0) {
        $GradleArgs = @("-version")
    }
    & $gradlew @GradleArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
