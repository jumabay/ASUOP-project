-- DRAFT ONLY — DO NOT EXECUTE.
-- P4-0 read stub: R1 day history (StateReportDayPort.findLinesForDay).
-- Target: RAILDB.RAIL_APP. Named binds. Hard page/limit for safety.
--
-- TIME AXIS (until DT1 closes): dislocation_ts = getting_ts / GETTING_DATE.
-- Do NOT filter on location_ts for the day window (DT1 BLOCKED).
-- Day bounds: half-open [D 00:00, D+1 00:00). TZ = R1-Q-TZ OPEN —
--   bind :day_from / :day_to as TIMESTAMP (wall-clock policy undecided).
-- NK1: retain duplicates; no DISTINCT / no UK collapse.
-- REST cap: 500 lines → 422 if more; stub fetches 501 to detect overflow.
--
-- Binds: :wagon_code, :day_from, :day_to, :fetch_limit (app: 501)
-- See docs/architecture/40 / 41 / 23 §day window.

SELECT
    NVL(e.legacy_report_id, 0) AS report_id,   -- PROVISIONAL; 0 if orphan/NULL
    e.wagon_code               AS wagon_code,
    e.dislocation_ts           AS dislocation_time,  -- getting_ts axis
    NVL(e.station_code, '')    AS station_code,
    NVL(e.operation_code, '')  AS operation_code,
    e.event_id                 AS event_id,
    e.location_ts              AS location_ts        -- attr only; not filter axis
FROM rail_disl_event e
JOIN rail_wagon w
    ON w.wagon_id = e.wagon_id
WHERE w.wagon_code = :wagon_code
  AND e.dislocation_ts >= :day_from
  AND e.dislocation_ts <  :day_to
ORDER BY e.dislocation_ts ASC, e.event_id ASC
FETCH FIRST :fetch_limit ROWS ONLY;
