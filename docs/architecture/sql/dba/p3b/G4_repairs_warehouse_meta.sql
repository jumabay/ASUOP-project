-- P3B Group G4 — Repairs + warehouse/parts metadata (dictionary only) — RP1 / WH1

-- G4.1 Repair-related columns
SELECT table_name, column_id, column_name, data_type, nullable
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN ('REPAIRS', 'REPAIRTYPES', 'REPAIRPLANTS', 'PLAN_REPAIRS_T')
ORDER BY table_name, column_id;
-- Export: p3b_G4_repair_columns_<date>.csv
-- RP1: note begin/end date column exact names (beginingdate spelling etc.).

-- G4.2 Triggers on REPAIRS / WAGON (CURRENTREPAIR linkage clues)
SELECT trigger_name, table_name, triggering_event, status
FROM all_triggers
WHERE owner = 'DBTC'
  AND table_name IN ('REPAIRS', 'WAGON')
ORDER BY table_name, trigger_name;
-- Export: p3b_G4_repair_triggers_<date>.csv

-- G4.3 Warehouse / parts columns
SELECT table_name, column_id, column_name, data_type, nullable
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN (
        'WAREHOUSES', 'WAREHOUSEDETAILS', 'VIRTUALWAREHOUSEDETAILS',
        'CURRENTPARTS', 'WAGONPARTS', 'PARTTYPES',
        'ARRIVALDOCUMENTS', 'PROCUREMENT', 'TRANSFERDOCUMENTS', 'WRITINGOFFDOCUMENTS'
      )
ORDER BY table_name, column_id;
-- Export: p3b_G4_wh_columns_<date>.csv
-- WH1: candidate key columns for stock grain.

-- G4.4 Constraints on warehouse/parts
SELECT c.table_name, c.constraint_name, c.constraint_type, cc.column_name, cc.position
FROM all_constraints c
JOIN all_cons_columns cc
  ON c.owner = cc.owner AND c.constraint_name = cc.constraint_name
WHERE c.owner = 'DBTC'
  AND c.table_name IN ('WAREHOUSEDETAILS', 'CURRENTPARTS', 'WAGONPARTS', 'WAREHOUSES')
ORDER BY c.table_name, c.constraint_type, c.constraint_name, cc.position;
-- Export: p3b_G4_wh_constraints_<date>.csv
