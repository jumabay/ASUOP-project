package platform.oracle.migration.l0;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reconcile report comparable to {@code expected_reconcile.json} counts / watched / duplicates.
 */
public record ReconcileReport(
        String schemaVersion,
        String packId,
        LocalDateTime cutoverTs,
        LocalDateTime cutoffTs,
        List<String> gapsOpen,
        Map<String, Integer> counts,
        WatchedStatus watched,
        DedupSummary duplicates,
        ChronologySummary chronology,
        String dayBoundaryPolicy
) {
    public record WatchedStatus(String status, String tag, String reason, int items) {
    }

    public record DedupSummary(
            String policy,
            String nk1Status,
            boolean uniqueEventIds,
            boolean uniqueProvenance,
            List<DupGroup> groups
    ) {
    }

    public record DupGroup(String code, LocalDateTime gettingTs, int sourceCount, List<Long> eventIds) {
    }

    public record ChronologySummary(
            String axis,
            String dt1Status,
            String bootstrapPolicy,
            String r1QTzStatus,
            String r1QTzNote,
            boolean noFutureInPrimary
    ) {
    }

    public static ReconcileReport from(SelectionResult selection) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        selection.refs().forEach((kind, rows) -> counts.put("refs_" + kind, rows.size()));
        counts.put("wagons_FULL", countTag(selection.wagons(), SelectionTag.FULL));
        counts.put("current_FULL", countTag(selection.current(), SelectionTag.FULL));
        counts.put("stock_FULL", countTag(selection.stock(), SelectionTag.FULL));
        counts.put("repairs_OPEN", countTag(selection.repairs(), SelectionTag.OPEN));
        counts.put("repairs_12M", countTag(selection.repairs(), SelectionTag.TWELVE_M));
        counts.put("repairs_ARCHIVE_DBTC", countTag(selection.repairs(), SelectionTag.ARCHIVE_DBTC));
        counts.put("repairs_SKIP", countTag(selection.repairs(), SelectionTag.SKIP));
        counts.put("kits_12M", countTag(selection.kits(), SelectionTag.TWELVE_M));
        counts.put("kits_ARCHIVE_DBTC", countTag(selection.kits(), SelectionTag.ARCHIVE_DBTC));
        counts.put("kits_SKIP", countTag(selection.kits(), SelectionTag.SKIP));
        counts.put("tape_total", selection.tape().size());
        counts.put("tape_12M", countTag(selection.tape(), SelectionTag.TWELVE_M));
        counts.put("tape_BOOTSTRAP", countTag(selection.tape(), SelectionTag.BOOTSTRAP));
        counts.put("tape_ARCHIVE_DBTC", countTag(selection.tape(), SelectionTag.ARCHIVE_DBTC));
        counts.put("tape_SKIP", countTag(selection.tape(), SelectionTag.SKIP));
        counts.put("watched_items", selection.watched().items().size());

        List<TaggedEntity> primaryTape = selection.tape().stream()
                .filter(t -> t.tag() == SelectionTag.TWELVE_M || t.tag() == SelectionTag.BOOTSTRAP)
                .toList();

        Set<Long> eventIds = new LinkedHashSet<>();
        Set<String> provenance = new LinkedHashSet<>();
        boolean uniqueEvents = true;
        boolean uniqueProv = true;
        for (TaggedEntity row : selection.tape()) {
            if (!eventIds.add(row.id())) {
                uniqueEvents = false;
            }
            if (row.provenanceKey() != null && !provenance.add(row.provenanceKey())) {
                uniqueProv = false;
            }
        }

        Map<String, List<TaggedEntity>> byWagonGetting = primaryTape.stream()
                .collect(Collectors.groupingBy(
                        t -> t.code() + "|" + t.gettingTs(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<DupGroup> groups = new ArrayList<>();
        byWagonGetting.forEach((key, rows) -> {
            if (rows.size() > 1) {
                groups.add(new DupGroup(
                        rows.getFirst().code(),
                        rows.getFirst().gettingTs(),
                        rows.size(),
                        rows.stream().map(TaggedEntity::id).toList()));
            }
        });

        boolean noFuture = selection.tape().stream()
                .filter(t -> t.tag() == SelectionTag.TWELVE_M || t.tag() == SelectionTag.BOOTSTRAP)
                .noneMatch(t -> t.gettingTs() != null && !t.gettingTs().isBefore(selection.cutoverTs()));

        return new ReconcileReport(
                selection.schemaVersion(),
                selection.packId(),
                selection.cutoverTs(),
                selection.cutoffTs(),
                List.copyOf(selection.gapsOpen()),
                Map.copyOf(counts),
                new WatchedStatus(
                        selection.watched().status(),
                        selection.watched().tag().wireName(),
                        selection.watched().reason(),
                        selection.watched().items().size()),
                new DedupSummary(
                        "retain_all_no_DISTINCT",
                        "OPEN",
                        uniqueEvents,
                        uniqueProv,
                        List.copyOf(groups)),
                new ChronologySummary(
                        selection.axis(),
                        "BLOCKED",
                        "max_getting_strictly_before_cutoff_per_wagon",
                        "OPEN",
                        "Day bounds half-open [D,D+1) on getting_ts documented; UTC vs local undecided",
                        noFuture),
                DayBoundaryPolicy.DOCUMENTATION);
    }

    public Map<LocalDate, Integer> tapeByDaySelected() {
        Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
        for (TaggedEntity row : /* filled by caller via selection */ List.<TaggedEntity>of()) {
            Objects.requireNonNull(row);
        }
        return byDay;
    }

    public static Map<LocalDate, Integer> tapeByDaySelected(SelectionResult selection) {
        Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
        selection.tape().stream()
                .filter(t -> t.tag() == SelectionTag.TWELVE_M || t.tag() == SelectionTag.BOOTSTRAP)
                .filter(t -> t.gettingTs() != null)
                .forEach(t -> {
                    LocalDate day = t.gettingTs().toLocalDate();
                    byDay.merge(day, 1, Integer::sum);
                });
        return byDay;
    }

    private static int countTag(List<TaggedEntity> rows, SelectionTag tag) {
        int n = 0;
        for (TaggedEntity row : rows) {
            if (row.tag() == tag) {
                n++;
            }
        }
        return n;
    }
}
