-- P3B Group G1 — Table columns + PK/UK/FK (dictionary only)
-- Run only after G0 identity OK and visibility > 0.

-- G1.1 Columns (core + repairs/warehouse parents for later RP1/WH1)
SELECT owner, table_name, column_id, column_name, data_type,
       data_length, data_precision, data_scale, nullable
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN (
        'WAGON', 'IMPORTFILES', 'STATEREPORTS', 'STATEREPORTRECORDS',
        'REPAIRS', 'REPAIRTYPES', 'CURRENTPARTS', 'WAGONPARTS',
        'WAREHOUSEDETAILS', 'WAREHOUSES', 'STATIONS', 'WAGONMILEAGE',
        'STATEREPORTRECORDS_TMP', 'WAGONSTATEREPORTS', 'WAGONSTATEREPORTSRECORDS'
      )
ORDER BY table_name, column_id;
-- Export: p3b_G1_columns_<date>.csv
-- Goal: close column lists for NK1/DT1/RP1/WH1 candidates.
-- Privs: see ALL_TAB_COLUMNS for accessible DBTC tables.
-- Expected: many rows; STATEREPORTRECORDS wide. Empty for a listed table = visibility gap or missing object → note, do not conclude absence alone.

-- G1.2 Constraints
SELECT c.owner, c.table_name, c.constraint_name, c.constraint_type,
       c.r_owner, c.r_constraint_name, c.status,
       cc.column_name, cc.position
FROM all_constraints c
JOIN all_cons_columns cc
  ON c.owner = cc.owner AND c.constraint_name = cc.constraint_name
WHERE c.owner = 'DBTC'
  AND c.table_name IN (
        'WAGON', 'IMPORTFILES', 'STATEREPORTS', 'STATEREPORTRECORDS',
        'REPAIRS', 'REPAIRTYPES', 'CURRENTPARTS', 'WAGONPARTS',
        'WAREHOUSEDETAILS', 'WAREHOUSES', 'STATIONS'
      )
ORDER BY c.table_name, c.constraint_type, c.constraint_name, cc.position;
-- Export: p3b_G1_constraints_<date>.csv
-- Goal: PK/UK/FK; expect NO PK on STATEREPORTRECORDS (NK1).
-- Expected: WAGON/IMPORTFILES/STATEREPORTS have PK ID pattern.
