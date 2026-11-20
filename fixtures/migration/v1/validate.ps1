# Offline structural validation for fixtures/migration/v1. No Oracle.
$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$failed = 0

function Fail([string]$msg) {
  Write-Host "FAIL: $msg" -ForegroundColor Red
  $script:failed++
}
function Ok([string]$msg) { Write-Host "OK:   $msg" -ForegroundColor Green }

$required = @(
  "manifest.json", "refs.json", "wagons.json", "current.json", "repairs.json",
  "stock.json", "kits.json", "tape.json", "expected_selection.json", "expected_reconcile.json",
  "schema/selection_result.schema.json", "schema/reconcile_report.schema.json", "README.md"
)
foreach ($f in $required) {
  $p = Join-Path $Root $f
  if (-not (Test-Path $p)) { Fail "missing $f" } else { Ok "present $f" }
}

function Read-Json([string]$rel) {
  $raw = Get-Content (Join-Path $Root $rel) -Raw -Encoding UTF8
  return ($raw | ConvertFrom-Json)
}

$manifest = Read-Json "manifest.json"
$wagons = Read-Json "wagons.json"
$current = Read-Json "current.json"
$repairs = Read-Json "repairs.json"
$stock = Read-Json "stock.json"
$kits = Read-Json "kits.json"
$tape = Read-Json "tape.json"
$sel = Read-Json "expected_selection.json"
$rec = Read-Json "expected_reconcile.json"

if ($manifest.cutover_ts -ne "2026-11-16T00:00:00") { Fail "cutover_ts mismatch" } else { Ok "cutover_ts" }
if ($manifest.cutoff_ts -ne "2025-11-16T00:00:00") { Fail "cutoff_ts mismatch" } else { Ok "cutoff_ts" }
if ($sel.cutoff_ts -ne $manifest.cutoff_ts) { Fail "selection cutoff != manifest" } else { Ok "selection window sync" }
if ($rec.cutoff_ts -ne $manifest.cutoff_ts) { Fail "reconcile cutoff != manifest" } else { Ok "reconcile window sync" }

$gaps = @($sel.gaps_open)
foreach ($g in @("NK1", "DT1", "G31", "R1-Q-TZ")) {
  if ($gaps -notcontains $g) { Fail "gaps_open missing $g" } else { Ok "gap open: $g" }
}

if ($wagons.wagons.Count -ne $sel.wagons.Count) { Fail "wagon count source vs selection" } else { Ok "wagon counts" }
if (($sel.wagons | Where-Object tag -ne "FULL").Count -gt 0) { Fail "non-FULL wagon tag" } else { Ok "wagons FULL" }
if (($sel.current | Where-Object tag -ne "FULL").Count -gt 0) { Fail "non-FULL current tag" } else { Ok "current FULL" }
if (($sel.stock | Where-Object tag -ne "FULL").Count -gt 0) { Fail "non-FULL stock tag" } else { Ok "stock FULL" }

if ($sel.watched.status -ne "BLOCKED") { Fail "watched not BLOCKED" } else { Ok "watched BLOCKED" }
if ($sel.watched.items.Count -ne 0) { Fail "watched items not empty" } else { Ok "watched empty" }
if ($rec.counts.watched_items -ne 0) { Fail "reconcile watched_items != 0" } else { Ok "reconcile watched_items=0" }

# Open repair must match current pointer
$openIds = @($sel.repairs | Where-Object tag -eq "OPEN" | ForEach-Object { $_.repair_id })
$curPointers = @($current.current | Where-Object { $null -ne $_.current_repair_id } | ForEach-Object { $_.current_repair_id })
foreach ($oid in $openIds) {
  if ($curPointers -notcontains $oid) { Fail "OPEN repair $oid not in current pointers" }
}
if ($openIds.Count -ne 1) { Fail "expected exactly 1 OPEN repair, got $($openIds.Count)" } else { Ok "OPEN via CURRENT (1)" }

# Tape tags partition
$tagSum = ($sel.tape | Group-Object tag | Measure-Object -Property Count -Sum).Sum
if ($tagSum -ne $tape.tape.Count) { Fail "tape selection size != source" } else { Ok "tape selection size" }
if ($rec.counts.tape_total -ne $tape.tape.Count) { Fail "reconcile tape_total" } else { Ok "reconcile tape_total" }
if ($rec.counts.tape_12M -ne @($sel.tape | Where-Object tag -eq "12M").Count) { Fail "tape_12M count" } else { Ok "tape_12M count" }
if ($rec.counts.tape_BOOTSTRAP -ne @($sel.tape | Where-Object tag -eq "BOOTSTRAP").Count) { Fail "tape_BOOTSTRAP count" } else { Ok "tape_BOOTSTRAP count" }

# Unique event_id / provenance
$eids = @($tape.tape | ForEach-Object { $_.event_id })
$provs = @($tape.tape | ForEach-Object { $_.provenance_key })
if (($eids | Select-Object -Unique).Count -ne $eids.Count) { Fail "duplicate event_id" } else { Ok "unique event_id" }
if (($provs | Select-Object -Unique).Count -ne $provs.Count) { Fail "duplicate provenance_key" } else { Ok "unique provenance_key" }

# Dup retain group present for W000006
$dup = @($rec.duplicates.groups | Where-Object { $_.code -eq "W000006" })
if ($dup.Count -lt 1) { Fail "missing W000006 duplicate group" } else { Ok "W000006 dup retain group" }
if ($rec.duplicates.policy -ne "retain_all_no_DISTINCT") { Fail "dup policy" } else { Ok "dup policy retain_all" }
if ($rec.duplicates.nk1_status -ne "OPEN") { Fail "NK1 must remain OPEN" } else { Ok "NK1 OPEN" }

# Bootstrap: one per wagon that has pre-cutoff, getting < cutoff
$cutoff = [datetime]$manifest.cutoff_ts
$boot = @($sel.tape | Where-Object tag -eq "BOOTSTRAP")
foreach ($b in $boot) {
  if ([datetime]$b.getting_ts -ge $cutoff) { Fail "BOOTSTRAP getting >= cutoff for $($b.code)" }
}
$bootCodes = @($boot | ForEach-Object { $_.code } | Select-Object -Unique)
if ($bootCodes.Count -ne $boot.Count) { Fail "multiple BOOTSTRAP per wagon" } else { Ok "one BOOTSTRAP per wagon ($($boot.Count))" }

# getting != location example exists
$gne = @($tape.tape | Where-Object { $_.code -eq "W000005" -and $_.getting_ts -ne $_.location_ts })
if ($gne.Count -lt 1) { Fail "missing getting!=location rows" } else { Ok "getting!=location present" }

# No synthetic watched codes from R3 catalog pattern SYN- in watched
if ($sel.watched.items.Count -gt 0) { Fail "watched must stay empty (G31)" }

# Codes are W###### synthetic
$badCodes = @($wagons.wagons | Where-Object { $_.code -notmatch '^W\d{6}$' })
if ($badCodes.Count -gt 0) { Fail "non-synthetic wagon codes" } else { Ok "wagon codes W######" }

# Exclusions present for archive / future
$exclReasons = @($rec.exclusions | ForEach-Object { $_.exclusion_reason } | Select-Object -Unique)
if ($exclReasons -notcontains "out_of_window_pre_cutoff") { Fail "missing out_of_window exclusions" } else { Ok "out_of_window exclusions" }
if ($exclReasons -notcontains "future_after_cutover") { Fail "missing future exclusions" } else { Ok "future exclusions" }

if ($failed -gt 0) {
  Write-Host "`nVALIDATION FAILED: $failed check(s)" -ForegroundColor Red
  exit 1
}
Write-Host "`nVALIDATION PASSED" -ForegroundColor Green
exit 0
