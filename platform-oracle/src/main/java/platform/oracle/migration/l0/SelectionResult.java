package platform.oracle.migration.l0;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline L0 selection snapshot — comparable to {@code expected_selection.json}.
 */
public record SelectionResult(
        String schemaVersion,
        String packId,
        LocalDateTime cutoverTs,
        LocalDateTime cutoffTs,
        String axis,
        List<String> gapsOpen,
        List<String> notes,
        Map<String, List<TaggedEntity>> refs,
        List<TaggedEntity> wagons,
        List<TaggedEntity> current,
        List<TaggedEntity> repairs,
        List<TaggedEntity> stock,
        List<TaggedEntity> kits,
        List<TaggedEntity> tape,
        WatchedSelection watched
) {
    public record WatchedSelection(
            String migrationPhase,
            String status,
            SelectionTag tag,
            String reason,
            String note,
            List<String> items
    ) {
    }

    public List<TaggedEntity> primaryLoad(List<TaggedEntity> rows) {
        return rows.stream()
                .filter(r -> r.tag() != SelectionTag.ARCHIVE_DBTC && r.tag() != SelectionTag.SKIP)
                .toList();
    }

    public Map<String, Integer> countByTag(List<TaggedEntity> rows) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TaggedEntity row : rows) {
            counts.merge(row.tagWire(), 1, Integer::sum);
        }
        return counts;
    }
}
