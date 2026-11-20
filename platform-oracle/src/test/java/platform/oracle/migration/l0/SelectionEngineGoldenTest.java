package platform.oracle.migration.l0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Golden comparison against {@code fixtures/migration/v1/expected_selection.json}
 * and {@code expected_reconcile.json}. Offline only.
 */
class SelectionEngineGoldenTest {

    private static MigrationFixturePack pack;
    private static SelectionResult selection;
    private static ReconcileReport reconcile;

    @BeforeAll
    static void load() throws Exception {
        Path dir = FixturePaths.requireV1();
        pack = MigrationFixturePack.load(dir);
        SelectionEngine engine = new SelectionEngine();
        selection = engine.select(pack);
        reconcile = engine.reconcile(selection);
    }

    @Test
    void windowMatchesManifest() {
        assertEquals(LocalDateTime.parse("2026-11-16T00:00:00"), selection.cutoverTs());
        assertEquals(LocalDateTime.parse("2025-11-16T00:00:00"), selection.cutoffTs());
        assertEquals(selection.cutoverTs().minusMonths(12), selection.cutoffTs());
        assertEquals("GETTING_DATE", selection.axis());
    }

    @Test
    void openGapsRemainOpen() {
        assertTrue(selection.gapsOpen().containsAll(OpenGaps.stillOpen()));
        assertEquals(OpenGaps.stillOpen(), selection.gapsOpen());
    }

    @Test
    void selectionTagsMatchExpected() {
        JsonNode expected = pack.expectedSelection();
        assertNotNull(expected, "expected_selection.json required");

        assertRefs(expected.get("refs"), selection.refs());
        assertTaggedById(expected.get("wagons"), selection.wagons(), "wagon_id");
        assertTaggedById(expected.get("current"), selection.current(), "wagon_id");
        assertTaggedById(expected.get("repairs"), selection.repairs(), "repair_id");
        assertTaggedById(expected.get("stock"), selection.stock(), "stock_id");
        assertTaggedById(expected.get("kits"), selection.kits(), "kit_id");
        assertTape(expected.get("tape"), selection.tape());
    }

    @Test
    void watchedIsBlockedSkipG31() {
        JsonNode expected = pack.expectedSelection().get("watched");
        assertEquals("BLOCKED", selection.watched().status());
        assertEquals(SelectionTag.SKIP, selection.watched().tag());
        assertEquals(expected.get("reason").asText(), selection.watched().reason());
        assertTrue(selection.watched().items().isEmpty());
        assertTrue(selection.gapsOpen().contains("G31"));
    }

    @Test
    void reconcileCountsMatchExpected() {
        JsonNode expected = pack.expectedReconcile();
        assertNotNull(expected, "expected_reconcile.json required");
        JsonNode counts = expected.get("counts");
        counts.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            int want = entry.getValue().asInt();
            assertEquals(want, reconcile.counts().getOrDefault(key, -1), "count " + key);
        });
    }

    @Test
    void reconcileWatchedAndDuplicates() {
        JsonNode expected = pack.expectedReconcile();
        assertEquals("BLOCKED", reconcile.watched().status());
        assertEquals("SKIP", reconcile.watched().tag());
        assertEquals(0, reconcile.watched().items());
        assertEquals("OPEN", reconcile.duplicates().nk1Status());
        assertTrue(reconcile.duplicates().uniqueEventIds());
        assertTrue(reconcile.duplicates().uniqueProvenance());
        assertTrue(reconcile.duplicates().policy().contains("retain_all"));

        JsonNode groups = expected.get("duplicates").get("groups");
        assertEquals(groups.size(), reconcile.duplicates().groups().size());
        JsonNode first = groups.get(0);
        ReconcileReport.DupGroup actual = reconcile.duplicates().groups().getFirst();
        assertEquals(first.get("code").asText(), actual.code());
        assertEquals(first.get("source_count").asInt(), actual.sourceCount());
        assertEquals(first.get("event_ids").size(), actual.eventIds().size());
    }

    @Test
    void dayBoundaryDocumentedAndTzOpen() {
        assertTrue(DayBoundaryPolicy.DOCUMENTATION.contains("half-open"));
        assertTrue(DayBoundaryPolicy.DOCUMENTATION.contains("R1-Q-TZ OPEN"));
        assertEquals("OPEN", reconcile.chronology().r1QTzStatus());
        assertEquals("BLOCKED", reconcile.chronology().dt1Status());
        assertTrue(reconcile.chronology().noFutureInPrimary());
    }

    @Test
    void bootstrapStrictlyBeforeCutoff() {
        for (TaggedEntity row : selection.tape()) {
            if (row.tag() == SelectionTag.BOOTSTRAP) {
                assertTrue(row.gettingTs().isBefore(selection.cutoffTs()),
                        "bootstrap must be < cutoff: " + row.id());
            }
            if (row.tag() == SelectionTag.TWELVE_M) {
                assertFalse(row.gettingTs().isBefore(selection.cutoffTs()));
                assertTrue(row.gettingTs().isBefore(selection.cutoverTs()));
            }
        }
    }

    @Test
    void intentionalDuplicatesRetained() {
        long dups = selection.tape().stream()
                .filter(t -> "W000006".equals(t.code()))
                .filter(t -> LocalDateTime.parse("2026-04-01T10:00:00").equals(t.gettingTs()))
                .filter(t -> t.tag() == SelectionTag.TWELVE_M)
                .count();
        assertEquals(3, dups);
        Set<String> keys = new HashSet<>();
        selection.tape().stream()
                .filter(t -> "W000006".equals(t.code()))
                .filter(t -> LocalDateTime.parse("2026-04-01T10:00:00").equals(t.gettingTs()))
                .forEach(t -> assertTrue(keys.add(t.provenanceKey())));
    }

    private static void assertRefs(JsonNode expected, Map<String, java.util.List<TaggedEntity>> actual) {
        Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            java.util.List<TaggedEntity> rows = actual.get(entry.getKey());
            assertNotNull(rows, "missing ref kind " + entry.getKey());
            assertEquals(entry.getValue().size(), rows.size(), "ref count " + entry.getKey());
            for (JsonNode node : entry.getValue()) {
                long id = node.get("id").asLong();
                TaggedEntity found = rows.stream().filter(r -> r.id() == id).findFirst().orElseThrow();
                assertEquals("REF_FULL", found.tagWire());
                assertEquals(node.get("code").asText(), found.code());
            }
        }
    }

    private static void assertTaggedById(JsonNode expected, java.util.List<TaggedEntity> actual, String idField) {
        assertEquals(expected.size(), actual.size(), "size for " + idField);
        for (JsonNode node : expected) {
            long id = node.get(idField).asLong();
            TaggedEntity found = actual.stream().filter(r -> r.id() == id).findFirst()
                    .orElseThrow(() -> new AssertionError("missing " + idField + "=" + id));
            assertEquals(node.get("tag").asText(), found.tagWire(), idField + "=" + id);
            String expectedReason = node.has("exclusion_reason") && !node.get("exclusion_reason").isNull()
                    ? node.get("exclusion_reason").asText()
                    : null;
            assertEquals(expectedReason, found.exclusionReason(), "exclusion " + idField + "=" + id);
        }
    }

    private static void assertTape(JsonNode expected, java.util.List<TaggedEntity> actual) {
        assertEquals(expected.size(), actual.size(), "tape size");
        for (JsonNode node : expected) {
            long id = node.get("event_id").asLong();
            TaggedEntity found = actual.stream().filter(r -> r.id() == id).findFirst()
                    .orElseThrow(() -> new AssertionError("missing event_id=" + id));
            assertEquals(node.get("tag").asText(), found.tagWire(), "event " + id);
            assertEquals(node.get("provenance_key").asText(), found.provenanceKey(), "provenance " + id);
            String expectedReason = node.has("exclusion_reason") && !node.get("exclusion_reason").isNull()
                    ? node.get("exclusion_reason").asText()
                    : null;
            assertEquals(expectedReason, found.exclusionReason(), "exclusion event " + id);
        }
    }
}
