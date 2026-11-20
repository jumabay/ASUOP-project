-- DRAFT ONLY — DO NOT EXECUTE.
-- P4-0 read stub: R2 wagon registry page (WagonRegistryPort.findPage).
-- Target: RAILDB.RAIL_APP. Named binds. Page size API max = 200.
-- Binds: :search_pattern (nullable; use '%' for unfiltered — app decides),
--        :sort_desc (0=asc, 1=desc), :offset_rows, :page_size
-- See docs/architecture/40 / 41.

-- Page content
SELECT w.wagon_code AS wagon_code
FROM rail_wagon w
WHERE (:search_pattern IS NULL OR w.wagon_code LIKE :search_pattern)
ORDER BY
    CASE WHEN :sort_desc = 0 THEN w.wagon_code END ASC,
    CASE WHEN :sort_desc = 1 THEN w.wagon_code END DESC
OFFSET :offset_rows ROWS FETCH NEXT :page_size ROWS ONLY;

-- Companion count (same filter; run separately)
-- SELECT COUNT(*) AS total_elements
-- FROM rail_wagon w
-- WHERE (:search_pattern IS NULL OR w.wagon_code LIKE :search_pattern);
