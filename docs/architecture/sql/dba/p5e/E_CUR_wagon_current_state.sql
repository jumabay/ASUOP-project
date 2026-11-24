-- P5-E-CUR — WAGON master currentState candidates (DRAFT)
-- DO NOT EXECUTE without APPROVALS: «P5-E-CUR JDBC currentState CONTROL» = APPROVED.
-- Not part of P5-E / P5-E allow-list delta (those forbid currentState).
-- Anti-rules: do NOT select STATEREPORTRECORDS day lines; do NOT use GETTING_DATE / SYSDATE
-- as currentState.dislocationTime; do NOT silent-fallback LOCATIONDATE → REPORTDATE.

-- Bind :wagon_code = '54787361' (CONTROL)
SELECT
    w.code                                              AS wagon_code,
    w.locationstation_id                                AS location_station_id,
    ls.code                                             AS station_code,
    w.operation_id                                      AS operation_id,
    NVL(op.idcode, op.code)                             AS operation_code,
    w.locationdate                                      AS location_date,
    w.reportdate                                        AS report_date,
    CAST(NULL AS VARCHAR2(64))                          AS train_index_placeholder
FROM dbtc.wagon w
LEFT JOIN dbtc.stations ls
       ON ls.id = w.locationstation_id
LEFT JOIN dbtc.operations op
       ON op.id = w.operation_id
WHERE w.code = :wagon_code
FETCH FIRST 1 ROW ONLY;

-- Optional metadata (same gate): list WAGON columns matching TRAIN%
-- SELECT column_name, data_type
-- FROM all_tab_columns
-- WHERE owner = 'DBTC' AND table_name = 'WAGON' AND column_name LIKE '%TRAIN%'
-- ORDER BY column_id;
