-- DRAFT ONLY — DO NOT EXECUTE.
-- P4-0 read stub: R1 wagon card by code (WagonCardPort.findByCode).
-- Target: RAILDB.RAIL_APP. Named binds. No live JDBC until APPROVED.
-- See docs/architecture/40_P4_0_RAILDB_STORAGE_CONTRACTS.md
--      docs/architecture/41_P4_0_R1_R3_FIELD_MAPPING.md

SELECT
    w.wagon_code          AS wagon_code,
    w.model_code          AS model,
    w.wagon_kind          AS wagon_kind,
    w.owner_name          AS owner_name,
    NVL(s.station_code, '') AS home_station_code,
    NVL(s.station_name, '') AS home_station_name,
    w.inventory_park      AS inventory_park,
    -- Optional current-state block (not in WagonCard DTO today):
    c.station_code        AS current_station_code,
    c.operation_code      AS current_operation_code,
    c.dislocation_ts      AS current_dislocation_ts,
    c.train_index         AS current_train_index
FROM rail_wagon w
LEFT JOIN rail_station s
    ON s.station_id = w.home_station_id
LEFT JOIN rail_wagon_current c
    ON c.wagon_id = w.wagon_id
WHERE w.wagon_code = :wagon_code
FETCH FIRST 1 ROW ONLY;
