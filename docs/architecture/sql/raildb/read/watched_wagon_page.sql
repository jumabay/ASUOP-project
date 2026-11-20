-- DRAFT ONLY — DO NOT EXECUTE.
-- P4-0 read stub: R3 watched wagon page (WatchedWagonPort.findPage).
-- Target: RAILDB.RAIL_APP. Named binds. Page size API max = 200.
--
-- G31 OPEN: rail_wagon_watched empty until approved membership snapshot.
-- This query is NOT proven equivalent to legacy GetActiveWagonList.
-- Synthetic fixture (60 codes) is NOT a migration source.
--
-- Binds: :search_pattern, :sort_desc (0/1), :offset_rows, :page_size
-- See docs/architecture/40 / 41 / analysis/11-nuff-active-wagon-list.md

SELECT
    w.wagon_code          AS wagon_code,
    w.model_code          AS model,
    w.wagon_kind          AS wagon_kind,
    w.owner_name          AS owner_name,
    NVL(s.station_code, '') AS home_station_code,
    NVL(s.station_name, '') AS home_station_name,
    w.inventory_park      AS inventory_park,
    m.source_code         AS watch_source_code,
    m.captured_at_utc     AS watch_captured_at_utc
FROM rail_wagon_watched m
JOIN rail_wagon w
    ON w.wagon_id = m.wagon_id
LEFT JOIN rail_station s
    ON s.station_id = w.home_station_id
WHERE (:search_pattern IS NULL OR w.wagon_code LIKE :search_pattern)
ORDER BY
    CASE WHEN :sort_desc = 0 THEN w.wagon_code END ASC,
    CASE WHEN :sort_desc = 1 THEN w.wagon_code END DESC
OFFSET :offset_rows ROWS FETCH NEXT :page_size ROWS ONLY;

-- Companion count (same filter; run separately)
-- SELECT COUNT(*) AS total_elements
-- FROM rail_wagon_watched m
-- JOIN rail_wagon w ON w.wagon_id = m.wagon_id
-- WHERE (:search_pattern IS NULL OR w.wagon_code LIKE :search_pattern);
