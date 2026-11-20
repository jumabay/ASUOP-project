<#
.SYNOPSIS
  Hit R1–R3 REST endpoints on a running synthetic platform-app (default http://localhost:8080).

.DESCRIPTION
  Prefer in-process Gradle smoke for CI:
    .\scripts\gradle-jdk21.ps1 :platform-app:test --tests "platform.app.api.SyntheticRuntimeSmokeTest"
  Use this script only when bootRun (or another local JVM) is already listening.

.EXAMPLE
  .\scripts\gradle-jdk21.ps1 :platform-app:bootRun
  # other terminal:
  .\scripts\rest-smoke-synthetic.ps1
  .\scripts\rest-smoke-synthetic.ps1 -BaseUrl http://127.0.0.1:8080
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Invoke-SmokeGet([string]$Path) {
    $uri = "$BaseUrl$Path"
    Write-Host "GET $uri"
    $response = Invoke-WebRequest -Uri $uri -Method GET -UseBasicParsing
    if ($response.StatusCode -ne 200) {
        throw "Expected HTTP 200 for $Path, got $($response.StatusCode)"
    }
    return $response.Content | ConvertFrom-Json
}

Write-Host "Synthetic REST smoke against $BaseUrl (profile synthetic expected)"

$card = Invoke-SmokeGet "/api/v1/wagons/SYN-00001"
if ($card.wagonCode -ne "SYN-00001") {
    throw "Card wagonCode mismatch: $($card.wagonCode)"
}

$day = Invoke-SmokeGet "/api/v1/wagons/SYN-00001/state-reports?date=2024-01-15"
if (-not $day.lines -or $day.lines.Count -lt 1) {
    throw "Expected lines on 2024-01-15"
}

$empty = Invoke-SmokeGet "/api/v1/wagons/SYN-00001/state-reports?date=2024-01-16"
$emptyCount = @($empty.lines).Count
if ($emptyCount -ne 0) {
    throw "Expected empty lines on 2024-01-16, got $emptyCount"
}

$registry = Invoke-SmokeGet "/api/v1/wagons"
if (-not $registry.content -or $registry.content.Count -lt 1) {
    throw "Registry content empty"
}

$watched = Invoke-SmokeGet "/api/v1/wagons/watched"
if (-not $watched.content -or $watched.content.Count -lt 1) {
    throw "Watched content empty"
}

Write-Host "PASSED: card, day history (data+empty), registry, watched"
exit 0
