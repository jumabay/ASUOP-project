-- P3B Group G2 — Indexes, partitions, optimizer stats (no COUNT on business tables)

-- G2.1 Index headers
SELECT owner, table_name, index_name, index_type, uniqueness, status, partitioned
FROM all_indexes
WHERE owner = 'DBTC'
  AND table_name IN (
        'WAGON', 'IMPORTFILES', 'STATEREPORTS', 'STATEREPORTRECORDS',
        'REPAIRS', 'CURRENTPARTS', 'WAGONPARTS', 'WAREHOUSEDETAILS', 'STATIONS'
      )
ORDER BY table_name, index_name;
-- Export: p3b_G2_indexes_<date>.csv

-- G2.2 Index columns (tape + kits + wagon + repairs) — DT1 / NK1 evidence
SELECT index_owner, table_name, index_name, column_position, column_name, descend
FROM all_ind_columns
WHERE index_owner = 'DBTC'
  AND table_name IN ('STATEREPORTRECORDS', 'WAGON', 'IMPORTFILES', 'STATEREPORTS', 'REPAIRS')
ORDER BY table_name, index_name, column_position;
-- Export: p3b_G2_ind_columns_<date>.csv
-- Look for GETTING_DATE vs LOCATION_DATE in index definitions (DT1).

-- G2.3 Partitions on DBTC
SELECT owner, table_name, partitioning_type, partition_count
FROM all_part_tables
WHERE owner = 'DBTC'
ORDER BY table_name;
-- Export: p3b_G2_partitions_<date>.csv
-- Expected: 0 rows (P3A). Empty alone OK if G0 visibility passed.

-- G2.4 Stats only (NUM_ROWS/BLOCKS) — never COUNT(*)
SELECT owner, table_name, num_rows, blocks, avg_row_len, partitioned, last_analyzed
FROM all_tables
WHERE owner = 'DBTC'
  AND table_name IN (
        'WAGON', 'IMPORTFILES', 'STATEREPORTS', 'STATEREPORTRECORDS',
        'REPAIRS', 'CURRENTPARTS', 'WAGONPARTS', 'WAREHOUSEDETAILS',
        'STATIONS', 'WAGONMILEAGE', 'STATEREPORTRECORDS_TMP',
        'WAGONSTATEREPORTS', 'WAGONSTATEREPORTSRECORDS'
      )
ORDER BY table_name;
-- Export: p3b_G2_stats_<date>.csv
