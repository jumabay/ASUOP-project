-- P3C WH1 — WAREHOUSEDETAILS OBJECT_TYPE / OBJECT_ID / OBJECT_STATE
-- DO NOT RUN until P3C-1. Table ~8983 rows (P3B) — full aggregates OK.
-- Timeout: 30s. Expected load: low (small table + small refs).
-- No Register* EXECUTE.

-- =============================================================================
-- WH1.1 Distinct OBJECT_TYPE values and row counts
-- =============================================================================
SELECT object_type, COUNT(*) AS row_cnt, SUM(NVL(object_count,0)) AS sum_count
FROM dbtc.warehousedetails
GROUP BY object_type
ORDER BY object_type;
-- Export: p3c_WH1_object_type_hist_<date>.csv

-- =============================================================================
-- WH1.2 Distinct OBJECT_STATE (+ FK buying type name if visible)
-- =============================================================================
SELECT wd.object_state,
       bt.id AS buyingtype_id,
       bt.name AS buyingtype_name,
       COUNT(*) AS row_cnt
FROM dbtc.warehousedetails wd
LEFT JOIN dbtc.buyingtypes bt
  ON bt.id = wd.object_state
GROUP BY wd.object_state, bt.id, bt.name
ORDER BY wd.object_state;
-- Export: p3c_WH1_object_state_hist_<date>.csv
-- If BUYINGTYPES missing grant → ORA-00942; fall back to state ids only (WH1.2b).

SELECT object_state, COUNT(*) AS row_cnt
FROM dbtc.warehousedetails
GROUP BY object_state
ORDER BY object_state;
-- Export: p3c_WH1_object_state_ids_<date>.csv

-- =============================================================================
-- WH1.3 Does OBJECT_ID join to WAGONPARTS.ID for dominant type?
-- Sample overlap counts (no SELECT *)
-- =============================================================================
SELECT COUNT(*) AS details_rows,
       SUM(CASE WHEN wp.id IS NOT NULL THEN 1 ELSE 0 END) AS object_id_in_wagonparts
FROM dbtc.warehousedetails wd
LEFT JOIN dbtc.wagonparts wp ON wp.id = wd.object_id;
-- Export: p3c_WH1_object_id_vs_parts_<date>.csv

-- Optional: overlap vs PARTTYPES.id (type-level stock?)
SELECT COUNT(*) AS details_rows,
       SUM(CASE WHEN pt.id IS NOT NULL THEN 1 ELSE 0 END) AS object_id_in_parttypes
FROM dbtc.warehousedetails wd
LEFT JOIN dbtc.parttypes pt ON pt.id = wd.object_id;
-- Export: p3c_WH1_object_id_vs_parttypes_<date>.csv

-- =============================================================================
-- WH1.4 UK sanity: duplicate UK keys must be 0
-- =============================================================================
SELECT *
FROM (
  SELECT warehouse_id, object_id, object_state, COUNT(*) AS cnt
  FROM dbtc.warehousedetails
  GROUP BY warehouse_id, object_id, object_state
  HAVING COUNT(*) > 1
)
WHERE ROWNUM <= 50;
-- Export: p3c_WH1_uk_violations_<date>.csv (expect 0)

-- =============================================================================
-- WH1.5 Warehouse header + type for migration FULL snapshot
-- =============================================================================
SELECT w.id AS warehouse_id,
       w.name,
       w.repairplant_id,
       w.warehousetype_id,
       COUNT(d.object_id) AS detail_rows
FROM dbtc.warehouses w
LEFT JOIN dbtc.warehousedetails d ON d.warehouse_id = w.id
GROUP BY w.id, w.name, w.repairplant_id, w.warehousetype_id
ORDER BY w.id;
-- Export: p3c_WH1_warehouse_summary_<date>.csv

-- Migration draft: FULL snapshot of WAREHOUSEDETAILS at cutover (age-independent).
-- Document history 12м by DELIVERYDATE is separate (headers already known).
