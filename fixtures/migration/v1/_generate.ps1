# Offline L0 fixture generator for P4-1 (C). Synthetic only. No Oracle.
# Run from repo root or this directory. Overwrites golden JSON in fixtures/migration/v1.

$ErrorActionPreference = "Stop"
$OutDir = $PSScriptRoot

$cutover = [datetime]::Parse("2026-11-16T00:00:00")
$cutoff  = $cutover.AddMonths(-12) # 2025-11-16

function Iso([datetime]$d) { $d.ToString("yyyy-MM-ddTHH:mm:ss") }
function DayIso([datetime]$d) { $d.ToString("yyyy-MM-dd") }

# --- refs (REF_FULL) ---
$stations = @(
  @{ id = 1001; code = "6900"; name = "SYN Station Alpha" },
  @{ id = 1002; code = "6901"; name = "SYN Station Beta" },
  @{ id = 1003; code = "6902"; name = "SYN Station Gamma" },
  @{ id = 1004; code = "7100"; name = "SYN Station Delta" },
  @{ id = 1005; code = "7200"; name = "SYN Station Epsilon" }
)
$wagonTypes = @(
  @{ id = 1; code = "KR"; name = "Covered (syn)" },
  @{ id = 2; code = "PL"; name = "Platform (syn)" },
  @{ id = 3; code = "HV"; name = "Hopper (syn)" }
)
$operations = @(
  @{ id = 1; code = "01"; name = "Arrival (syn)" },
  @{ id = 2; code = "03"; name = "Departure (syn)" },
  @{ id = 3; code = "10"; name = "Loading (syn)" }
)
$repairTypes = @(
  @{ id = 0; code = "DR"; name = "Depot repair (syn)" },
  @{ id = 1; code = "KR"; name = "Capital repair (syn)" },
  @{ id = 7; code = "TR1"; name = "TR-1 (syn)" }
)
$warehouses = @(
  @{ id = 501; code = "WH-A"; name = "Synthetic warehouse A" },
  @{ id = 502; code = "WH-B"; name = "Synthetic warehouse B" }
)
$partTypes = @(
  @{ id = 11; code = "AXLE"; name = "Axle (syn)" },
  @{ id = 12; code = "WHEEL"; name = "Wheel pair (syn)" }
)

$refs = [ordered]@{
  schema_version = "migration-l0-v1"
  stations       = $stations
  wagon_types    = $wagonTypes
  operations     = $operations
  repair_types   = $repairTypes
  warehouses     = $warehouses
  part_types     = $partTypes
}

# --- wagons (FULL regardless of create date) ---
# Scenarios encoded in notes for Backend SelectionEngine consumers.
$wagonDefs = @(
  @{ code = "W000001"; id = 1;  create = "2018-03-01T10:00:00"; typeId = 1; scenario = "old_create_active_full" },
  @{ code = "W000002"; id = 2;  create = "2026-01-15T08:00:00"; typeId = 2; scenario = "create_inside_12m_full" },
  @{ code = "W000003"; id = 3;  create = "2015-06-20T12:00:00"; typeId = 1; scenario = "archive_bootstrap_12m_tape" },
  @{ code = "W000004"; id = 4;  create = "2010-01-01T00:00:00"; typeId = 3; scenario = "archive_only_needs_bootstrap" },
  @{ code = "W000005"; id = 5;  create = "2019-08-08T09:00:00"; typeId = 2; scenario = "getting_ne_location" },
  @{ code = "W000006"; id = 6;  create = "2017-11-11T11:11:11"; typeId = 1; scenario = "intentional_duplicates" },
  @{ code = "W000007"; id = 7;  create = "2016-04-04T04:00:00"; typeId = 3; scenario = "open_repair_old_begin" },
  @{ code = "W000008"; id = 8;  create = "2020-02-02T02:00:00"; typeId = 1; scenario = "closed_repair_in_12m" },
  @{ code = "W000009"; id = 9;  create = "2014-09-09T09:00:00"; typeId = 2; scenario = "closed_repair_pre_cutoff_archive" },
  @{ code = "W000010"; id = 10; create = "2021-05-05T05:00:00"; typeId = 1; scenario = "day_boundary_r1qtz_open" },
  @{ code = "W000011"; id = 11; create = "2013-07-07T07:00:00"; typeId = 3; scenario = "bootstrap_equals_cutoff" },
  @{ code = "W000012"; id = 12; create = "2022-12-12T12:00:00"; typeId = 2; scenario = "kit_12m_and_archive" },
  @{ code = "W000013"; id = 13; create = "2011-01-01T01:00:00"; typeId = 1; scenario = "no_tape_before_cutoff_no_bootstrap" },
  @{ code = "W000014"; id = 14; create = "2018-10-10T10:00:00"; typeId = 2; scenario = "multiple_archive_bootstrap_pick_max" },
  @{ code = "W000015"; id = 15; create = "2019-03-03T03:00:00"; typeId = 3; scenario = "stock_full_old_wagon" },
  @{ code = "W000016"; id = 16; create = "2025-11-16T00:00:00"; typeId = 1; scenario = "create_exactly_cutoff_full" },
  @{ code = "W000017"; id = 17; create = "2024-06-01T06:00:00"; typeId = 2; scenario = "future_getting_excluded" },
  @{ code = "W000018"; id = 18; create = "2012-02-02T02:00:00"; typeId = 1; scenario = "kit_orphan_soft_null" },
  @{ code = "W000019"; id = 19; create = "2023-08-08T08:00:00"; typeId = 3; scenario = "dense_12m_hour_strip" },
  @{ code = "W000020"; id = 20; create = "2015-05-05T05:00:00"; typeId = 2; scenario = "spare_active_minimal_tape" }
)

$wagons = @()
foreach ($w in $wagonDefs) {
  $wagons += [ordered]@{
    wagon_id      = $w.id
    code          = $w.code
    create_ts     = $w.create
    wagon_type_id = $w.typeId
    owner_code    = "SYN-OWNER"
    scenario      = $w.scenario
  }
}

# --- current (FULL) ---
# currentrepair_id links OPEN repairs for W000007
$current = @()
foreach ($w in $wagonDefs) {
  $curRepair = $null
  if ($w.code -eq "W000007") { $curRepair = 7001 }
  $current += [ordered]@{
    wagon_id           = $w.id
    code               = $w.code
    station_id         = 1001 + (($w.id - 1) % 5)
    operation_id       = 1 + (($w.id - 1) % 3)
    location_ts        = "2026-11-15T18:00:00"
    current_repair_id  = $curRepair
  }
}

# --- repairs ---
# OPEN via CURRENT pointer (RP1), ignore begin age.
# Closed 12M by endingdate in window; closed pre-cutoff → ARCHIVE_DBTC.
$repairs = @(
  [ordered]@{ repair_id = 7001; wagon_id = 7; code = "W000007"; repair_type_id = 0; begin_ts = "2020-01-10T00:00:00"; ending_ts = $null; is_open_via_current = $true; scenario = "open_old_begin" },
  [ordered]@{ repair_id = 7002; wagon_id = 7; code = "W000007"; repair_type_id = 7; begin_ts = "2019-05-01T00:00:00"; ending_ts = "2019-06-01T00:00:00"; is_open_via_current = $false; scenario = "closed_pre_cutoff_same_wagon" },
  [ordered]@{ repair_id = 8001; wagon_id = 8; code = "W000008"; repair_type_id = 1; begin_ts = "2026-02-01T00:00:00"; ending_ts = "2026-02-20T00:00:00"; is_open_via_current = $false; scenario = "closed_in_12m" },
  [ordered]@{ repair_id = 8002; wagon_id = 8; code = "W000008"; repair_type_id = 0; begin_ts = "2024-01-01T00:00:00"; ending_ts = "2024-02-01T00:00:00"; is_open_via_current = $false; scenario = "closed_pre_cutoff" },
  [ordered]@{ repair_id = 9001; wagon_id = 9; code = "W000009"; repair_type_id = 0; begin_ts = "2018-03-01T00:00:00"; ending_ts = "2018-04-01T00:00:00"; is_open_via_current = $false; scenario = "closed_archive_only" },
  [ordered]@{ repair_id = 10001; wagon_id = 10; code = "W000010"; repair_type_id = 7; begin_ts = "2025-12-01T00:00:00"; ending_ts = "2025-12-15T00:00:00"; is_open_via_current = $false; scenario = "closed_near_cutoff_in_12m" },
  [ordered]@{ repair_id = 15001; wagon_id = 15; code = "W000015"; repair_type_id = 1; begin_ts = "2026-06-01T00:00:00"; ending_ts = $null; is_open_via_current = $false; scenario = "ending_null_but_not_current_pointer" }
)

# --- stock FULL ---
$stock = @(
  [ordered]@{ stock_id = 1; warehouse_id = 501; part_type_id = 11; object_wagon_id = 15; qty = 2; scenario = "stock_on_old_wagon" },
  [ordered]@{ stock_id = 2; warehouse_id = 501; part_type_id = 12; object_wagon_id = 1; qty = 4; scenario = "stock_on_active" },
  [ordered]@{ stock_id = 3; warehouse_id = 502; part_type_id = 11; object_wagon_id = 7; qty = 1; scenario = "stock_open_repair_wagon" },
  [ordered]@{ stock_id = 4; warehouse_id = 502; part_type_id = 12; object_wagon_id = $null; qty = 10; scenario = "stock_no_wagon_fk" },
  [ordered]@{ stock_id = 5; warehouse_id = 501; part_type_id = 11; object_wagon_id = 20; qty = 3; scenario = "stock_spare" }
)

# --- kits ---
# Axis for kit header selection: getting_ts / import_ts comparable to cutoff.
$kits = @(
  [ordered]@{ kit_id = 301; filename = "syn_kit_12m_001.dat"; type = 0; import_ts = "2026-01-10T01:00:00"; getting_ts = "2026-01-10T00:00:00"; scenario = "kit_in_12m" },
  [ordered]@{ kit_id = 302; filename = "syn_kit_12m_002.dat"; type = 1; import_ts = "2025-11-16T00:00:00"; getting_ts = "2025-11-16T00:00:00"; scenario = "kit_on_cutoff_included_12m" },
  [ordered]@{ kit_id = 303; filename = "syn_kit_arch_001.dat"; type = 0; import_ts = "2025-05-01T00:00:00"; getting_ts = "2025-05-01T00:00:00"; scenario = "kit_pre_cutoff_archive" },
  [ordered]@{ kit_id = 304; filename = "syn_kit_arch_002.dat"; type = 0; import_ts = "2024-01-01T00:00:00"; getting_ts = "2024-01-01T00:00:00"; scenario = "kit_deep_archive" },
  [ordered]@{ kit_id = 305; filename = "syn_kit_12m_003.dat"; type = 0; import_ts = "2026-06-15T12:00:00"; getting_ts = "2026-06-15T12:00:00"; scenario = "kit_12m_mid" },
  [ordered]@{ kit_id = 306; filename = "syn_kit_12m_001.dat"; type = 0; import_ts = "2026-01-10T01:05:00"; getting_ts = "2026-01-10T00:00:00"; scenario = "kit_uk_replay_same_filename_type" },
  [ordered]@{ kit_id = 307; filename = "syn_kit_boot_edge.dat"; type = 1; import_ts = "2025-11-15T23:00:00"; getting_ts = "2025-11-15T23:00:00"; scenario = "kit_just_before_cutoff_archive" },
  [ordered]@{ kit_id = 308; filename = "syn_kit_orphan.dat"; type = 0; import_ts = "2026-03-01T00:00:00"; getting_ts = "2026-03-01T00:00:00"; scenario = "kit_12m_for_orphan_tape" }
)

# --- tape ---
# Axis: getting_ts (GETTING_DATE). location_ts may differ (DT1 still OPEN).
# Tags: 12M if getting >= cutoff; ARCHIVE_DBTC if getting < cutoff except BOOTSTRAP (max <= cutoff per wagon).
# Duplicates retained with distinct event_id + provenance_key.
# No future getting in primary load (exclude / ARCHIVE reason).

$tape = [System.Collections.Generic.List[object]]::new()
$eventSeq = 10000

function Add-Tape {
  param(
    [string]$Code,
    [int]$WagonId,
    [datetime]$Getting,
    [datetime]$Location,
    [int]$StationId,
    [int]$OpId,
    [nullable[int]]$KitId,
    [string]$Scenario,
    [string]$ProvSuffix = ""
  )
  $script:eventSeq++
  $eid = $script:eventSeq
  $prov = "SYN|TAPE|$Code|$(Iso $Getting)|$eid$ProvSuffix"
  $tape.Add([ordered]@{
    event_id       = $eid
    wagon_id       = $WagonId
    code           = $Code
    getting_ts     = (Iso $Getting)
    location_ts    = (Iso $Location)
    station_id     = $StationId
    operation_id   = $OpId
    kit_id         = $KitId
    provenance_key = $prov
    content_hash   = ("h" + $eid.ToString("x8"))
    scenario       = $Scenario
  }) | Out-Null
}

# W000001: mix archive + 12M + bootstrap candidate
Add-Tape "W000001" 1 ([datetime]"2024-06-01T10:00:00") ([datetime]"2024-06-01T10:00:00") 1001 1 304 "archive_deep"
Add-Tape "W000001" 1 ([datetime]"2025-10-01T10:00:00") ([datetime]"2025-10-01T10:00:00") 1002 2 303 "archive_near_cutoff_bootstrap_candidate"
Add-Tape "W000001" 1 ([datetime]"2025-11-16T00:00:00") ([datetime]"2025-11-16T00:00:00") 1001 1 302 "on_cutoff_12m"
Add-Tape "W000001" 1 ([datetime]"2026-05-01T08:00:00") ([datetime]"2026-05-01T08:00:00") 1003 1 301 "in_12m"

# W000002: only 12M (create inside window) — no bootstrap needed if first event >= cutoff
Add-Tape "W000002" 2 ([datetime]"2026-01-20T08:00:00") ([datetime]"2026-01-20T08:00:00") 1002 1 301 "in_12m_only"
Add-Tape "W000002" 2 ([datetime]"2026-08-01T12:00:00") ([datetime]"2026-08-01T12:00:00") 1002 2 305 "in_12m_late"

# W000003: archive / bootstrap / 12M
Add-Tape "W000003" 3 ([datetime]"2023-01-01T00:00:00") ([datetime]"2023-01-01T00:00:00") 1003 1 304 "archive_old"
Add-Tape "W000003" 3 ([datetime]"2025-06-15T12:00:00") ([datetime]"2025-06-15T12:00:00") 1003 2 303 "archive_not_max"
Add-Tape "W000003" 3 ([datetime]"2025-09-01T09:00:00") ([datetime]"2025-09-01T09:00:00") 1004 1 303 "archive_bootstrap_pick"
Add-Tape "W000003" 3 ([datetime]"2025-11-20T10:00:00") ([datetime]"2025-11-20T10:00:00") 1001 1 302 "in_12m"
Add-Tape "W000003" 3 ([datetime]"2026-03-03T03:00:00") ([datetime]"2026-03-03T03:00:00") 1001 3 305 "in_12m"

# W000004: archive only → bootstrap = max <= cutoff
Add-Tape "W000004" 4 ([datetime]"2022-04-04T04:00:00") ([datetime]"2022-04-04T04:00:00") 1004 1 304 "archive_only_a"
Add-Tape "W000004" 4 ([datetime]"2025-01-01T00:00:00") ([datetime]"2025-01-01T00:00:00") 1005 2 303 "archive_only_bootstrap"

# W000005: getting ≠ location (DT1 OPEN — selection still on getting)
Add-Tape "W000005" 5 ([datetime]"2026-02-10T06:00:00") ([datetime]"2026-02-11T18:00:00") 1001 1 301 "getting_ne_location_12m"
Add-Tape "W000005" 5 ([datetime]"2025-08-01T06:00:00") ([datetime]"2025-08-02T18:00:00") 1002 2 303 "getting_ne_location_archive_bootstrap"

# W000006: intentional duplicates same getting_ts — retain all (NK1 OPEN)
Add-Tape "W000006" 6 ([datetime]"2026-04-01T10:00:00") ([datetime]"2026-04-01T10:00:00") 1001 1 305 "dup_a" "-A"
Add-Tape "W000006" 6 ([datetime]"2026-04-01T10:00:00") ([datetime]"2026-04-01T10:00:00") 1001 1 305 "dup_b" "-B"
Add-Tape "W000006" 6 ([datetime]"2026-04-01T10:00:00") ([datetime]"2026-04-01T10:05:00") 1002 2 305 "dup_c_diff_location" "-C"
Add-Tape "W000006" 6 ([datetime]"2025-07-01T10:00:00") ([datetime]"2025-07-01T10:00:00") 1003 1 303 "dup_archive_bootstrap"

# W000007: open repair wagon tape
Add-Tape "W000007" 7 ([datetime]"2025-11-17T00:00:00") ([datetime]"2025-11-17T00:00:00") 1001 1 302 "in_12m"
Add-Tape "W000007" 7 ([datetime]"2025-05-05T05:00:00") ([datetime]"2025-05-05T05:00:00") 1002 2 303 "archive_bootstrap"

# W000008 / W000009
Add-Tape "W000008" 8 ([datetime]"2026-02-15T00:00:00") ([datetime]"2026-02-15T00:00:00") 1002 1 301 "in_12m"
Add-Tape "W000008" 8 ([datetime]"2025-02-15T00:00:00") ([datetime]"2025-02-15T00:00:00") 1002 1 303 "archive_bootstrap"
Add-Tape "W000009" 9 ([datetime]"2024-04-01T00:00:00") ([datetime]"2024-04-01T00:00:00") 1003 1 304 "archive_bootstrap"
Add-Tape "W000009" 9 ([datetime]"2026-01-01T00:00:00") ([datetime]"2026-01-01T00:00:00") 1003 2 301 "in_12m"

# W000010: day boundary half-open [D,D+1) — R1-Q-TZ OPEN (document only)
Add-Tape "W000010" 10 ([datetime]"2026-07-01T00:00:00") ([datetime]"2026-07-01T00:00:00") 1001 1 305 "day_start_inclusive_candidate"
Add-Tape "W000010" 10 ([datetime]"2026-07-01T23:59:59") ([datetime]"2026-07-01T23:59:59") 1001 2 305 "day_end_same_calendar_day"
Add-Tape "W000010" 10 ([datetime]"2026-07-02T00:00:00") ([datetime]"2026-07-02T00:00:00") 1001 1 305 "next_day_start"
Add-Tape "W000010" 10 ([datetime]"2025-11-15T23:59:59") ([datetime]"2025-11-15T23:59:59") 1002 1 307 "just_before_cutoff_bootstrap"

# W000011: bootstrap exactly at cutoff (also qualifies as 12M — tag 12M wins for in-window; bootstrap not needed if on-cutoff exists)
# Per rules: History >= cutoff → 12M. Bootstrap is last getting <= cutoff when needed for hole.
# Event ON cutoff is 12M. Also add earlier archive for bootstrap-max demo on another path.
Add-Tape "W000011" 11 ([datetime]"2025-11-16T00:00:00") ([datetime]"2025-11-16T00:00:00") 1004 1 302 "exactly_cutoff_12m"
Add-Tape "W000011" 11 ([datetime]"2025-11-15T12:00:00") ([datetime]"2025-11-15T12:00:00") 1004 2 307 "pre_cutoff_archive_not_bootstrap_if_on_cutoff_covers"

# W000012: kit linkage 12m + archive
Add-Tape "W000012" 12 ([datetime]"2026-06-15T12:00:00") ([datetime]"2026-06-15T12:00:00") 1005 1 305 "kit_12m_linked"
Add-Tape "W000012" 12 ([datetime]"2025-05-01T00:00:00") ([datetime]"2025-05-01T00:00:00") 1005 2 303 "kit_archive_linked_bootstrap"

# W000013: only 12M events (no pre-cutoff) → no BOOTSTRAP row
Add-Tape "W000013" 13 ([datetime]"2026-09-01T09:00:00") ([datetime]"2026-09-01T09:00:00") 1001 1 305 "in_12m_no_bootstrap_needed"

# W000014: multiple archive → bootstrap = max getting <= cutoff
Add-Tape "W000014" 14 ([datetime]"2020-01-01T00:00:00") ([datetime]"2020-01-01T00:00:00") 1002 1 304 "arch_old"
Add-Tape "W000014" 14 ([datetime]"2024-06-01T00:00:00") ([datetime]"2024-06-01T00:00:00") 1002 2 304 "arch_mid"
Add-Tape "W000014" 14 ([datetime]"2025-11-01T00:00:00") ([datetime]"2025-11-01T00:00:00") 1002 1 303 "arch_bootstrap_max"
Add-Tape "W000014" 14 ([datetime]"2025-10-01T00:00:00") ([datetime]"2025-10-01T00:00:00") 1002 3 303 "arch_not_max"
Add-Tape "W000014" 14 ([datetime]"2026-10-01T00:00:00") ([datetime]"2026-10-01T00:00:00") 1002 1 305 "in_12m"

# W000015
Add-Tape "W000015" 15 ([datetime]"2026-04-04T04:00:00") ([datetime]"2026-04-04T04:00:00") 1003 1 301 "in_12m"
Add-Tape "W000015" 15 ([datetime]"2025-03-03T03:00:00") ([datetime]"2025-03-03T03:00:00") 1003 2 303 "archive_bootstrap"

# W000016
Add-Tape "W000016" 16 ([datetime]"2025-11-16T12:00:00") ([datetime]"2025-11-16T12:00:00") 1001 1 302 "in_12m"
Add-Tape "W000016" 16 ([datetime]"2026-11-01T00:00:00") ([datetime]"2026-11-01T00:00:00") 1001 2 305 "in_12m_near_cutover"

# W000017: future getting after cutover → exclude from primary load
Add-Tape "W000017" 17 ([datetime]"2026-11-16T01:00:00") ([datetime]"2026-11-16T01:00:00") 1002 1 305 "future_after_cutover_exclude"
Add-Tape "W000017" 17 ([datetime]"2026-10-10T10:00:00") ([datetime]"2026-10-10T10:00:00") 1002 2 305 "in_12m_ok"
Add-Tape "W000017" 17 ([datetime]"2025-06-01T00:00:00") ([datetime]"2025-06-01T00:00:00") 1002 1 303 "archive_bootstrap"

# W000018: orphan kit_id soft-null (KIT_FK UNKNOWN at 12m scale)
Add-Tape "W000018" 18 ([datetime]"2026-03-01T00:00:00") ([datetime]"2026-03-01T00:00:00") 1004 1 $null "kit_id_null_soft"
Add-Tape "W000018" 18 ([datetime]"2026-03-02T00:00:00") ([datetime]"2026-03-02T00:00:00") 1004 2 9999 "kit_id_orphan_unknown_fk"
Add-Tape "W000018" 18 ([datetime]"2025-04-01T00:00:00") ([datetime]"2025-04-01T00:00:00") 1004 1 304 "archive_bootstrap"

# W000019: dense 12M strip (~48 hourly samples) for volume without claiming scale
$base = [datetime]"2026-05-01T00:00:00"
for ($h = 0; $h -lt 48; $h++) {
  $g = $base.AddHours($h)
  Add-Tape "W000019" 19 $g $g 1005 (1 + ($h % 3)) 305 "dense_12m_hour"
}
Add-Tape "W000019" 19 ([datetime]"2025-08-15T00:00:00") ([datetime]"2025-08-15T00:00:00") 1005 1 303 "archive_bootstrap"

# W000020: minimal
Add-Tape "W000020" 20 ([datetime]"2026-01-01T00:00:00") ([datetime]"2026-01-01T00:00:00") 1001 1 301 "in_12m"
Add-Tape "W000020" 20 ([datetime]"2025-01-01T00:00:00") ([datetime]"2025-01-01T00:00:00") 1001 2 304 "archive_bootstrap"

# --- compute expected selection tags ---

function Parse-Iso([string]$s) { [datetime]::Parse($s) }

# Bootstrap: per wagon, among tape with getting < cutoff OR getting <= cutoff when no 12M covers hole —
# Spec: BOOTSTRAP = last getting_ts ≤ cutoff per wagon (when needed to seed window edge).
# Policy encoded here (L0 contract):
#   - Tag every tape with getting_ts >= cutoff AND getting_ts < cutover as 12M
#   - Tag tape with getting_ts >= cutover as SKIP (exclusion: future_after_cutover)
#   - Among remaining getting_ts <= cutoff (i.e. < cutoff OR == handled above): ARCHIVE_DBTC by default
#   - For each wagon that has at least one event with getting_ts < cutoff OR needs seed:
#       BOOTSTRAP = the single event with max getting_ts among those with getting_ts <= cutoff
#         BUT if that max is also >= cutoff (i.e. exactly cutoff), it is already 12M — do NOT dual-tag;
#         bootstrap only applies to events with getting_ts < cutoff (strict) when the wagon has
#         no event with getting_ts == cutoff? 
# Doc 42: "BOOTSTRAP | Last getting_ts ≤ cutoff per wagon"
# Doc 44: "History < cutoff → ARCHIVE except BOOTSTRAP (max ≤ cutoff per wagon as needed)"
# So BOOTSTRAP is subset of pre-cutoff (getting <= cutoff), specifically max <= cutoff.
# If max <= cutoff is exactly cutoff, that row is History >= cutoff → 12M, and bootstrap "as needed"
# is satisfied by the 12M on-cutoff row — no separate BOOTSTRAP tag on a pre-cutoff row required
# unless there is a hole (no event >= cutoff at the edge). Practical L0 rule used here:
#   BOOTSTRAP = argmax getting among events with getting_ts < cutoff (strictly before window),
#   only if wagon has ANY event with getting_ts < cutoff.
#   Events with getting_ts == cutoff are 12M (not BOOTSTRAP).
#   Other pre-cutoff events = ARCHIVE_DBTC with exclusion_reason out_of_window_pre_cutoff.

$cutoffDt = $cutoff
$cutoverDt = $cutover

$tapeExpected = @()

# Manual per-wagon bootstrap (Group-Object is unreliable on OrderedDictionary rows).
$bootstrapEventIds = @{}
$preByWagon = @{}
foreach ($row in $tape) {
  $g = Parse-Iso $row.getting_ts
  if ($g -lt $cutoffDt) {
    $code = [string]$row.code
    if (-not $preByWagon.ContainsKey($code)) { $preByWagon[$code] = @() }
    $preByWagon[$code] += ,$row
  }
}
foreach ($code in @($preByWagon.Keys)) {
  $pre = @($preByWagon[$code])
  $maxGetting = ($pre | ForEach-Object { Parse-Iso $_.getting_ts } | Measure-Object -Maximum).Maximum
  $candidates = @($pre | Where-Object { (Parse-Iso $_.getting_ts) -eq $maxGetting })
  $boot = $candidates | Sort-Object { [int]$_.event_id } -Descending | Select-Object -First 1
  $bootstrapEventIds[$code] = [int]$boot.event_id
}

foreach ($row in $tape) {
  $g = Parse-Iso $row.getting_ts
  $code = [string]$row.code
  $eid = [int]$row.event_id
  $tag = $null
  $excl = $null
  if ($g -ge $cutoverDt) {
    $tag = "SKIP"
    $excl = "future_after_cutover"
  }
  elseif ($g -ge $cutoffDt) {
    $tag = "12M"
  }
  elseif ($bootstrapEventIds.ContainsKey($code) -and $bootstrapEventIds[$code] -eq $eid) {
    $tag = "BOOTSTRAP"
  }
  else {
    $tag = "ARCHIVE_DBTC"
    $excl = "out_of_window_pre_cutoff"
  }
  $tapeExpected += [ordered]@{
    event_id         = $eid
    code             = $code
    getting_ts       = $row.getting_ts
    tag              = $tag
    exclusion_reason = $excl
    provenance_key   = $row.provenance_key
  }
}

# kits selection
$kitsExpected = @()
foreach ($k in $kits) {
  $g = Parse-Iso $k.getting_ts
  if ($g -ge $cutoffDt -and $g -lt $cutoverDt) {
    $kitsExpected += [ordered]@{ kit_id = $k.kit_id; filename = $k.filename; type = $k.type; tag = "12M"; exclusion_reason = $null }
  }
  elseif ($g -ge $cutoverDt) {
    $kitsExpected += [ordered]@{ kit_id = $k.kit_id; filename = $k.filename; type = $k.type; tag = "SKIP"; exclusion_reason = "future_after_cutover" }
  }
  else {
    $kitsExpected += [ordered]@{ kit_id = $k.kit_id; filename = $k.filename; type = $k.type; tag = "ARCHIVE_DBTC"; exclusion_reason = "out_of_window_pre_cutoff" }
  }
}

# repairs selection
$repairsExpected = @()
foreach ($r in $repairs) {
  if ($r.is_open_via_current) {
    $repairsExpected += [ordered]@{ repair_id = $r.repair_id; code = $r.code; tag = "OPEN"; exclusion_reason = $null }
  }
  elseif ($null -eq $r.ending_ts) {
    # ending null but NOT current pointer → not OPEN; treat as SKIP/unknown until RP1 — encode as ARCHIVE_DBTC? 
    # Doc: OPEN only via CURRENT pointer. Null ending alone ≠ OPEN. Exclude from OPEN/12M primary as SKIP with reason.
    $repairsExpected += [ordered]@{ repair_id = $r.repair_id; code = $r.code; tag = "SKIP"; exclusion_reason = "ending_null_without_current_pointer" }
  }
  else {
    $e = Parse-Iso $r.ending_ts
    if ($e -ge $cutoffDt -and $e -lt $cutoverDt) {
      $repairsExpected += [ordered]@{ repair_id = $r.repair_id; code = $r.code; tag = "12M"; exclusion_reason = $null }
    }
    elseif ($e -ge $cutoverDt) {
      $repairsExpected += [ordered]@{ repair_id = $r.repair_id; code = $r.code; tag = "SKIP"; exclusion_reason = "future_after_cutover" }
    }
    else {
      $repairsExpected += [ordered]@{ repair_id = $r.repair_id; code = $r.code; tag = "ARCHIVE_DBTC"; exclusion_reason = "out_of_window_pre_cutoff" }
    }
  }
}

$wagonsExpected = @($wagons | ForEach-Object { [ordered]@{ code = $_.code; wagon_id = $_.wagon_id; tag = "FULL"; exclusion_reason = $null } })
$currentExpected = @($current | ForEach-Object { [ordered]@{ code = $_.code; wagon_id = $_.wagon_id; tag = "FULL"; exclusion_reason = $null } })
$stockExpected = @($stock | ForEach-Object { [ordered]@{ stock_id = $_.stock_id; tag = "FULL"; exclusion_reason = $null } })

$refsExpected = [ordered]@{
  stations     = @($stations | ForEach-Object { [ordered]@{ id = $_.id; code = $_.code; tag = "REF_FULL" } })
  wagon_types  = @($wagonTypes | ForEach-Object { [ordered]@{ id = $_.id; code = $_.code; tag = "REF_FULL" } })
  operations   = @($operations | ForEach-Object { [ordered]@{ id = $_.id; code = $_.code; tag = "REF_FULL" } })
  repair_types = @($repairTypes | ForEach-Object { [ordered]@{ id = $_.id; code = $_.code; tag = "REF_FULL" } })
  warehouses   = @($warehouses | ForEach-Object { [ordered]@{ id = $_.id; code = $_.code; tag = "REF_FULL" } })
  part_types   = @($partTypes | ForEach-Object { [ordered]@{ id = $_.id; code = $_.code; tag = "REF_FULL" } })
}

$watchedExpected = [ordered]@{
  migration_phase = "M8"
  status          = "BLOCKED"
  tag             = "SKIP"
  reason          = "G31_source_unapproved"
  note            = "Do not use R3 synthetic watched.json as M8 source"
  items           = @()
}

$expectedSelection = [ordered]@{
  schema_version = "migration-l0-v1"
  pack_id        = "migration-l0-v1"
  cutover_ts     = (Iso $cutover)
  cutoff_ts      = (Iso $cutoff)
  axis           = "GETTING_DATE"
  gaps_open      = @("NK1", "DT1", "G31", "R1-Q-TZ")
  notes          = @(
    "cutoff = cutover_ts - 12 calendar months",
    "Tape axis = getting_ts until DT1 closes",
    "Duplicates retained; no DISTINCT collapse (NK1 OPEN)",
    "BOOTSTRAP = max getting_ts strictly < cutoff per wagon that has pre-cutoff events",
    "Watched M8 BLOCKED / empty (G31)",
    "NK1 DT1 G31 R1-Q-TZ remain OPEN — do not claim closed"
  )
  refs     = $refsExpected
  wagons   = $wagonsExpected
  current  = $currentExpected
  repairs  = $repairsExpected
  stock    = $stockExpected
  kits     = $kitsExpected
  tape     = $tapeExpected
  watched  = $watchedExpected
}

# --- reconcile report ---
function Count-Tag($arr, $tag) { @($arr | Where-Object { $_.tag -eq $tag }).Count }

$tapeByDay = @{}
foreach ($t in $tapeExpected | Where-Object { $_.tag -eq "12M" -or $_.tag -eq "BOOTSTRAP" }) {
  $day = (Parse-Iso $t.getting_ts).ToString("yyyy-MM-dd")
  if (-not $tapeByDay.ContainsKey($day)) { $tapeByDay[$day] = 0 }
  $tapeByDay[$day]++
}
$tapeByDayArr = @($tapeByDay.GetEnumerator() | Sort-Object Name | ForEach-Object {
  [ordered]@{ day = $_.Key; selected_count = $_.Value }
})

$dupMap = @{}
foreach ($row in $tape) {
  $key = "$($row.code)|$($row.getting_ts)"
  if (-not $dupMap.ContainsKey($key)) { $dupMap[$key] = @() }
  $dupMap[$key] += ,$row
}
$dupReport = @()
foreach ($key in @($dupMap.Keys | Sort-Object)) {
  $grp = @($dupMap[$key])
  if ($grp.Count -gt 1) {
    $dupReport += [ordered]@{
      code            = [string]$grp[0].code
      getting_ts      = [string]$grp[0].getting_ts
      source_count    = $grp.Count
      event_ids       = @($grp | ForEach-Object { [int]$_.event_id })
      provenance_keys = @($grp | ForEach-Object { [string]$_.provenance_key })
      policy          = "retain_all"
    }
  }
}

$bootstrapRows = @($tapeExpected | Where-Object { $_.tag -eq "BOOTSTRAP" } | ForEach-Object {
  [ordered]@{ code = $_.code; event_id = $_.event_id; getting_ts = $_.getting_ts }
})

$exclusions = @($tapeExpected + $kitsExpected + $repairsExpected | Where-Object { $null -ne $_.exclusion_reason } | ForEach-Object {
  $id = $null
  if ($_.PSObject.Properties.Name -contains "event_id") { $id = "event:$($_.event_id)" }
  elseif ($_.PSObject.Properties.Name -contains "kit_id") { $id = "kit:$($_.kit_id)" }
  elseif ($_.PSObject.Properties.Name -contains "repair_id") { $id = "repair:$($_.repair_id)" }
  [ordered]@{
    ref              = $id
    tag              = $_.tag
    exclusion_reason = $_.exclusion_reason
  }
})

# kit UK replay note: filename+type collision 301 vs 306
$kitUk = @(
  [ordered]@{
    filename      = "syn_kit_12m_001.dat"
    type          = 0
    kit_ids       = @(301, 306)
    policy        = "idempotent_header_uk_filename_type"
    expected_headers_after_replay = 1
    note          = "L0 documents expectation; physical replay is M6 (not asserted as executed)"
  }
)

$kitIdSet = @{}
foreach ($k in $kits) { $kitIdSet[[int]$k.kit_id] = $true }
$orphanTape = @()
foreach ($row in $tape) {
  if ($null -eq $row.kit_id) { continue }
  $kid = [int]$row.kit_id
  if (-not $kitIdSet.ContainsKey($kid)) {
    $orphanTape += [ordered]@{
      event_id = [int]$row.event_id
      code     = [string]$row.code
      kit_id   = $kid
      policy   = "soft_null_or_flag_orphan"
      gap      = "KIT_FK_12m_UNKNOWN"
    }
  }
}

$expectedReconcile = [ordered]@{
  schema_version = "migration-l0-v1"
  pack_id        = "migration-l0-v1"
  cutover_ts     = (Iso $cutover)
  cutoff_ts      = (Iso $cutoff)
  gaps_open      = @("NK1", "DT1", "G31", "R1-Q-TZ")
  counts         = [ordered]@{
    refs_stations      = $stations.Count
    refs_wagon_types   = $wagonTypes.Count
    refs_operations    = $operations.Count
    refs_repair_types  = $repairTypes.Count
    refs_warehouses    = $warehouses.Count
    refs_part_types    = $partTypes.Count
    wagons_FULL        = (Count-Tag $wagonsExpected "FULL")
    current_FULL       = (Count-Tag $currentExpected "FULL")
    stock_FULL         = (Count-Tag $stockExpected "FULL")
    repairs_OPEN       = (Count-Tag $repairsExpected "OPEN")
    repairs_12M        = (Count-Tag $repairsExpected "12M")
    repairs_ARCHIVE_DBTC = (Count-Tag $repairsExpected "ARCHIVE_DBTC")
    repairs_SKIP       = (Count-Tag $repairsExpected "SKIP")
    kits_12M           = (Count-Tag $kitsExpected "12M")
    kits_ARCHIVE_DBTC  = (Count-Tag $kitsExpected "ARCHIVE_DBTC")
    kits_SKIP          = (Count-Tag $kitsExpected "SKIP")
    tape_total         = $tape.Count
    tape_12M           = (Count-Tag $tapeExpected "12M")
    tape_BOOTSTRAP     = (Count-Tag $tapeExpected "BOOTSTRAP")
    tape_ARCHIVE_DBTC  = (Count-Tag $tapeExpected "ARCHIVE_DBTC")
    tape_SKIP          = (Count-Tag $tapeExpected "SKIP")
    watched_items      = 0
  }
  watched = [ordered]@{
    status = "BLOCKED"
    tag    = "SKIP"
    reason = "G31"
    items  = @()
  }
  duplicates = [ordered]@{
    policy            = "retain_all_no_DISTINCT"
    nk1_status        = "OPEN"
    groups            = $dupReport
    unique_event_ids  = (($tape | ForEach-Object { $_.event_id } | Select-Object -Unique).Count -eq $tape.Count)
    unique_provenance = (($tape | ForEach-Object { $_.provenance_key } | Select-Object -Unique).Count -eq $tape.Count)
  }
  chronology = [ordered]@{
    axis                    = "GETTING_DATE"
    dt1_status              = "BLOCKED"
    bootstrap_policy        = "max_getting_strictly_before_cutoff_per_wagon"
    bootstrap_rows          = $bootstrapRows
    no_future_in_primary    = $true
    r1_q_tz_status          = "OPEN"
    r1_q_tz_note            = "Day bounds half-open [D,D+1) on getting_ts documented; UTC vs local undecided"
    getting_ne_location_examples = @(
      [ordered]@{ code = "W000005"; note = "selection uses getting_ts; location_ts may differ (DT1 OPEN)" }
    )
  }
  tape_by_day_selected = $tapeByDayArr
  kit_idempotency      = $kitUk
  orphans              = [ordered]@{
    kit_fk_gap = "KIT_FK_12m_UNKNOWN"
    tape_orphan_kit_ids = $orphanTape
  }
  exclusions = $exclusions
  invariants = @(
    "All wagons/current/stock tagged FULL",
    "Open repairs only via current_repair_id pointer (RP1)",
    "Tape duplicates retained with distinct event_id and provenance_key",
    "Watched empty and BLOCKED until G31",
    "Do not claim NK1 DT1 G31 R1-Q-TZ closed"
  )
}

$manifest = [ordered]@{
  pack_id        = "migration-l0-v1"
  schema_version = "migration-l0-v1"
  cutover_ts     = (Iso $cutover)
  cutoff_ts      = (Iso $cutoff)
  axis           = "GETTING_DATE"
  synthetic      = $true
  source         = "hand-crafted-generator"
  files = @(
    "manifest.json",
    "refs.json",
    "wagons.json",
    "current.json",
    "repairs.json",
    "stock.json",
    "kits.json",
    "tape.json",
    "expected_selection.json",
    "expected_reconcile.json",
    "schema/selection_result.schema.json",
    "schema/reconcile_report.schema.json",
    "README.md"
  )
  gaps_open = @("NK1", "DT1", "G31", "R1-Q-TZ")
  denied    = @("Create RAILDB", "Flyway", "DBTC read", "RAILDB write", "JDBC", "live migrate")
}

function Write-Json($path, $obj) {
  $json = $obj | ConvertTo-Json -Depth 20
  # PowerShell ConvertTo-Json can emit awkward formatting; still valid JSON
  [System.IO.File]::WriteAllText($path, $json, [System.Text.UTF8Encoding]::new($false))
}

Write-Json (Join-Path $OutDir "manifest.json") $manifest
Write-Json (Join-Path $OutDir "refs.json") $refs
Write-Json (Join-Path $OutDir "wagons.json") ([ordered]@{ schema_version = "migration-l0-v1"; wagons = $wagons })
Write-Json (Join-Path $OutDir "current.json") ([ordered]@{ schema_version = "migration-l0-v1"; current = $current })
Write-Json (Join-Path $OutDir "repairs.json") ([ordered]@{ schema_version = "migration-l0-v1"; repairs = $repairs })
Write-Json (Join-Path $OutDir "stock.json") ([ordered]@{ schema_version = "migration-l0-v1"; stock = $stock })
Write-Json (Join-Path $OutDir "kits.json") ([ordered]@{ schema_version = "migration-l0-v1"; kits = $kits })
Write-Json (Join-Path $OutDir "tape.json") ([ordered]@{ schema_version = "migration-l0-v1"; axis = "GETTING_DATE"; tape = $tape })
Write-Json (Join-Path $OutDir "expected_selection.json") $expectedSelection
Write-Json (Join-Path $OutDir "expected_reconcile.json") $expectedReconcile

Write-Host "Wrote fixture pack to $OutDir"
Write-Host ("tape rows={0} 12M={1} BOOTSTRAP={2} ARCHIVE={3} SKIP={4}" -f `
  $tape.Count, (Count-Tag $tapeExpected "12M"), (Count-Tag $tapeExpected "BOOTSTRAP"), `
  (Count-Tag $tapeExpected "ARCHIVE_DBTC"), (Count-Tag $tapeExpected "SKIP"))
