-- P3C-1 executable: WH1 warehouse semantics (~8983 rows OK)

SELECT object_type, COUNT(*) AS row_cnt, SUM(NVL(object_count,0)) AS sum_count
FROM dbtc.warehousedetails
GROUP BY object_type
ORDER BY object_type;

SELECT wd.object_state,
       bt.id AS buyingtype_id,
       bt.name AS buyingtype_name,
       COUNT(*) AS row_cnt
FROM dbtc.warehousedetails wd
LEFT JOIN dbtc.buyingtypes bt
  ON bt.id = wd.object_state
GROUP BY wd.object_state, bt.id, bt.name
ORDER BY wd.object_state;

SELECT object_state, COUNT(*) AS row_cnt
FROM dbtc.warehousedetails
GROUP BY object_state
ORDER BY object_state;

SELECT COUNT(*) AS details_rows,
       SUM(CASE WHEN wp.id IS NOT NULL THEN 1 ELSE 0 END) AS object_id_in_wagonparts
FROM dbtc.warehousedetails wd
LEFT JOIN dbtc.wagonparts wp ON wp.id = wd.object_id;

SELECT COUNT(*) AS details_rows,
       SUM(CASE WHEN pt.id IS NOT NULL THEN 1 ELSE 0 END) AS object_id_in_parttypes
FROM dbtc.warehousedetails wd
LEFT JOIN dbtc.parttypes pt ON pt.id = wd.object_id;

SELECT *
FROM (
  SELECT warehouse_id, object_id, object_state, COUNT(*) AS cnt
  FROM dbtc.warehousedetails
  GROUP BY warehouse_id, object_id, object_state
  HAVING COUNT(*) > 1
)
WHERE ROWNUM <= 50;

SELECT w.id AS warehouse_id,
       w.name,
       w.repairplant_id,
       w.warehousetype_id,
       COUNT(d.object_id) AS detail_rows
FROM dbtc.warehouses w
LEFT JOIN dbtc.warehousedetails d ON d.warehouse_id = w.id
GROUP BY w.id, w.name, w.repairplant_id, w.warehousetype_id
ORDER BY w.id;
