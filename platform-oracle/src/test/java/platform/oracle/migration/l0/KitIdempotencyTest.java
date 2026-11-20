package platform.oracle.migration.l0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import platform.oracle.migration.l0.model.KitRecord;

/**
 * Kit UK {@code (filename, type)} → one header on replay (M6 policy unit).
 * Selection still tags both source rows 12M; collapse is replay idempotency.
 */
class KitIdempotencyTest {

    @Test
    void replaySameFilenameTypeKeepsOneHeader() throws Exception {
        MigrationFixturePack pack = MigrationFixturePack.load(FixturePaths.requireV1());
        List<KitRecord> kits = pack.kits();

        KitRecord first = kits.stream().filter(k -> k.kitId() == 301L).findFirst().orElseThrow();
        KitRecord replay = kits.stream().filter(k -> k.kitId() == 306L).findFirst().orElseThrow();
        assertEquals(first.filename(), replay.filename());
        assertEquals(first.type(), replay.type());

        KitIdempotencyIndex index = new KitIdempotencyIndex();
        assertNull(index.put(first));
        KitRecord collision = index.put(replay);
        assertNotNull(collision);
        assertSame(first, collision);
        assertEquals(1, index.size());
        assertEquals(301L, index.get(first.filename(), first.type()).kitId());

        // Second independent replay still collapses to the same header.
        assertSame(first, index.put(replay));
        assertEquals(1, index.size());
    }

    @Test
    void distinctFilenameOrTypeAreSeparateHeaders() throws Exception {
        Path dir = FixturePaths.requireV1();
        MigrationFixturePack pack = MigrationFixturePack.load(dir);
        KitIdempotencyIndex index = new KitIdempotencyIndex();
        int expectedUnique = 0;
        for (KitRecord kit : pack.kits()) {
            if (index.put(kit) == null) {
                expectedUnique++;
            }
        }
        // 8 kit rows, one UK collision (301 vs 306) → 7 headers
        assertEquals(7, expectedUnique);
        assertEquals(7, index.size());
    }
}
