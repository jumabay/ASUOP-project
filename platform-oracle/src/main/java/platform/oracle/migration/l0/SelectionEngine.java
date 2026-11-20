package platform.oracle.migration.l0;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import platform.oracle.migration.l0.model.CurrentStateRecord;
import platform.oracle.migration.l0.model.KitRecord;
import platform.oracle.migration.l0.model.RefRecord;
import platform.oracle.migration.l0.model.RepairRecord;
import platform.oracle.migration.l0.model.StockRecord;
import platform.oracle.migration.l0.model.TapeEventRecord;
import platform.oracle.migration.l0.model.WagonRecord;

/**
 * Offline Migration SelectionEngine L0 (docs 25/29/44 + fixtures/migration/v1).
 *
 * <ul>
 *   <li>History window: {@code [cutoff, cutover)} on GETTING axis (12 calendar months)</li>
 *   <li>Active objects: FULL regardless of create date</li>
 *   <li>Open repairs: OPEN via {@code current.current_repair_id} (RP1), ignore begin age</li>
 *   <li>BOOTSTRAP: max getting_ts <em>strictly before</em> cutoff per wagon</li>
 *   <li>Tape duplicates retained (NK1 OPEN); provenance/event ids preserved</li>
 *   <li>Watched/M8: BLOCKED + SKIP while G31 OPEN</li>
 * </ul>
 *
 * <p>No JDBC, Oracle, Flyway, or live integrations.
 */
public final class SelectionEngine {

    public static final String WATCHED_REASON = "G31_source_unapproved";
    public static final String EXCLUSION_PRE_CUTOFF = "out_of_window_pre_cutoff";
    public static final String EXCLUSION_FUTURE = "future_after_cutover";
    public static final String EXCLUSION_ENDING_NULL = "ending_null_without_current_pointer";

    public SelectionResult select(MigrationFixturePack pack) {
        Objects.requireNonNull(pack, "pack");
        LocalDateTime cutover = pack.cutoverTs();
        LocalDateTime cutoff = pack.cutoffTs();

        Map<String, List<TaggedEntity>> refs = tagRefs(pack.refs());
        List<TaggedEntity> wagons = tagWagons(pack.wagons());
        List<TaggedEntity> current = tagCurrent(pack.current());
        List<TaggedEntity> stock = tagStock(pack.stock());
        List<TaggedEntity> repairs = tagRepairs(pack.current(), pack.repairs(), cutoff, cutover);
        List<TaggedEntity> kits = tagKits(pack.kits(), cutoff, cutover);
        List<TaggedEntity> tape = tagTape(pack.tape(), cutoff, cutover);

        SelectionResult.WatchedSelection watched = new SelectionResult.WatchedSelection(
                "M8",
                "BLOCKED",
                SelectionTag.SKIP,
                WATCHED_REASON,
                "Do not use R3 synthetic watched.json as M8 source",
                List.of());

        return new SelectionResult(
                MigrationFixturePack.SCHEMA_VERSION,
                pack.packId(),
                cutover,
                cutoff,
                pack.axis(),
                List.copyOf(pack.gapsOpen()),
                List.of(
                        "cutoff = cutover_ts - 12 calendar months",
                        "Tape axis = getting_ts until DT1 closes",
                        "Duplicates retained; no DISTINCT collapse (NK1 OPEN)",
                        "BOOTSTRAP = max getting_ts strictly < cutoff per wagon that has pre-cutoff events",
                        "Watched M8 BLOCKED / empty (G31)",
                        "NK1 DT1 G31 R1-Q-TZ remain OPEN — do not claim closed"),
                refs,
                wagons,
                current,
                repairs,
                stock,
                kits,
                tape,
                watched);
    }

    public ReconcileReport reconcile(SelectionResult selection) {
        return ReconcileReport.from(selection);
    }

    private static Map<String, List<TaggedEntity>> tagRefs(Map<String, List<RefRecord>> refs) {
        Map<String, List<TaggedEntity>> out = new LinkedHashMap<>();
        refs.forEach((kind, rows) -> {
            List<TaggedEntity> tagged = new ArrayList<>();
            for (RefRecord ref : rows) {
                tagged.add(TaggedEntity.ref(ref.id(), ref.code()));
            }
            tagged.sort(Comparator.comparing(TaggedEntity::id));
            out.put(kind, List.copyOf(tagged));
        });
        return Map.copyOf(out);
    }

    private static List<TaggedEntity> tagWagons(List<WagonRecord> wagons) {
        List<TaggedEntity> out = new ArrayList<>();
        for (WagonRecord wagon : wagons) {
            out.add(TaggedEntity.of(wagon.wagonId(), wagon.code(), SelectionTag.FULL, null));
        }
        out.sort(Comparator.comparing(TaggedEntity::id));
        return List.copyOf(out);
    }

    private static List<TaggedEntity> tagCurrent(List<CurrentStateRecord> current) {
        List<TaggedEntity> out = new ArrayList<>();
        for (CurrentStateRecord row : current) {
            out.add(TaggedEntity.of(row.wagonId(), row.code(), SelectionTag.FULL, null));
        }
        out.sort(Comparator.comparing(TaggedEntity::id));
        return List.copyOf(out);
    }

    private static List<TaggedEntity> tagStock(List<StockRecord> stock) {
        List<TaggedEntity> out = new ArrayList<>();
        for (StockRecord row : stock) {
            out.add(TaggedEntity.of(row.stockId(), null, SelectionTag.FULL, null));
        }
        out.sort(Comparator.comparing(TaggedEntity::id));
        return List.copyOf(out);
    }

    private static List<TaggedEntity> tagRepairs(
            List<CurrentStateRecord> current,
            List<RepairRecord> repairs,
            LocalDateTime cutoff,
            LocalDateTime cutover) {
        Set<Long> openIds = new HashSet<>();
        for (CurrentStateRecord row : current) {
            if (row.currentRepairId() != null) {
                openIds.add(row.currentRepairId());
            }
        }

        List<TaggedEntity> out = new ArrayList<>();
        for (RepairRecord repair : repairs) {
            boolean open = repair.isOpenViaCurrent() || openIds.contains(repair.repairId());
            if (open) {
                out.add(TaggedEntity.of(repair.repairId(), repair.code(), SelectionTag.OPEN, null));
                continue;
            }
            if (repair.endingTs() == null) {
                out.add(TaggedEntity.of(
                        repair.repairId(), repair.code(), SelectionTag.SKIP, EXCLUSION_ENDING_NULL));
                continue;
            }
            LocalDateTime ending = repair.endingTs();
            if (!ending.isBefore(cutoff) && ending.isBefore(cutover)) {
                out.add(TaggedEntity.of(repair.repairId(), repair.code(), SelectionTag.TWELVE_M, null));
            } else {
                out.add(TaggedEntity.of(
                        repair.repairId(), repair.code(), SelectionTag.ARCHIVE_DBTC, EXCLUSION_PRE_CUTOFF));
            }
        }
        out.sort(Comparator.comparing(TaggedEntity::id));
        return List.copyOf(out);
    }

    private static List<TaggedEntity> tagKits(
            List<KitRecord> kits, LocalDateTime cutoff, LocalDateTime cutover) {
        // Selection tags every kit row; UK collapse is a separate idempotency concern (M6).
        List<TaggedEntity> out = new ArrayList<>();
        for (KitRecord kit : kits) {
            LocalDateTime axis = kit.gettingTs() != null ? kit.gettingTs() : kit.importTs();
            if (axis != null && !axis.isBefore(cutoff) && axis.isBefore(cutover)) {
                out.add(TaggedEntity.kit(kit.kitId(), kit.filename(), kit.type(), SelectionTag.TWELVE_M, null));
            } else if (axis != null && !axis.isBefore(cutover)) {
                out.add(TaggedEntity.kit(
                        kit.kitId(), kit.filename(), kit.type(), SelectionTag.SKIP, EXCLUSION_FUTURE));
            } else {
                out.add(TaggedEntity.kit(
                        kit.kitId(), kit.filename(), kit.type(), SelectionTag.ARCHIVE_DBTC, EXCLUSION_PRE_CUTOFF));
            }
        }
        out.sort(Comparator.comparing(TaggedEntity::id));
        return List.copyOf(out);
    }

    private static List<TaggedEntity> tagTape(
            List<TapeEventRecord> events, LocalDateTime cutoff, LocalDateTime cutover) {
        // BOOTSTRAP = max getting_ts strictly < cutoff per wagon (doc 44 / fixture README).
        Map<String, TapeEventRecord> bootstrapByWagon = new HashMap<>();
        for (TapeEventRecord event : events) {
            LocalDateTime getting = event.gettingTs();
            if (getting == null || !getting.isBefore(cutoff)) {
                continue;
            }
            TapeEventRecord previous = bootstrapByWagon.get(event.code());
            if (previous == null
                    || getting.isAfter(previous.gettingTs())
                    || (getting.equals(previous.gettingTs())
                            && event.eventId() > previous.eventId())) {
                bootstrapByWagon.put(event.code(), event);
            }
        }
        Set<Long> bootstrapIds = new HashSet<>();
        for (TapeEventRecord bootstrap : bootstrapByWagon.values()) {
            bootstrapIds.add(bootstrap.eventId());
        }

        List<TaggedEntity> out = new ArrayList<>();
        for (TapeEventRecord event : events) {
            LocalDateTime getting = event.gettingTs();
            String provenance = TapeIdentity.provenanceKey(event);
            if (getting == null) {
                out.add(TaggedEntity.tape(
                        event.eventId(),
                        event.code(),
                        null,
                        SelectionTag.SKIP,
                        "missing_getting_ts",
                        provenance));
                continue;
            }
            if (!getting.isBefore(cutover)) {
                out.add(TaggedEntity.tape(
                        event.eventId(),
                        event.code(),
                        getting,
                        SelectionTag.SKIP,
                        EXCLUSION_FUTURE,
                        provenance));
            } else if (!getting.isBefore(cutoff)) {
                // [cutoff, cutover) → 12M (on-cutoff inclusive)
                out.add(TaggedEntity.tape(
                        event.eventId(), event.code(), getting, SelectionTag.TWELVE_M, null, provenance));
            } else if (bootstrapIds.contains(event.eventId())) {
                out.add(TaggedEntity.tape(
                        event.eventId(), event.code(), getting, SelectionTag.BOOTSTRAP, null, provenance));
            } else {
                out.add(TaggedEntity.tape(
                        event.eventId(),
                        event.code(),
                        getting,
                        SelectionTag.ARCHIVE_DBTC,
                        EXCLUSION_PRE_CUTOFF,
                        provenance));
            }
        }
        out.sort(Comparator.comparing(TaggedEntity::id));
        return List.copyOf(out);
    }
}
