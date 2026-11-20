package platform.oracle.dict.semantics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import platform.oracle.dict.semantics.DislEventProvenance.TapeRowRef;

class DislEventProvenanceTest {

    @Test
    void sameContentSameHash() {
        TapeRowRef a = row(10, 20, "2026-11-01T10:00:00", "2026-11-01T09:55:00", 1L, 2L, 3L, "IDX", 0);
        TapeRowRef b = row(10, 20, "2026-11-01T10:00:00", "2026-11-01T09:55:00", 1L, 2L, 3L, "IDX", 1);
        assertEquals(DislEventProvenance.contentHashSha256(a), DislEventProvenance.contentHashSha256(b));
        assertNotEquals(DislEventProvenance.provenanceKey(a), DislEventProvenance.provenanceKey(b));
    }

    @Test
    void differentStationDifferentHash() {
        TapeRowRef a = row(10, 20, "2026-11-01T10:00:00", "2026-11-01T09:55:00", 1L, 2L, 3L, "IDX", 0);
        TapeRowRef b = row(10, 20, "2026-11-01T10:00:00", "2026-11-01T09:55:00", 99L, 2L, 3L, "IDX", 1);
        assertTrue(DislEventProvenance.contentDiffers(a, b));
        assertTrue(DislEventProvenance.retainSourceRow(a));
        assertTrue(DislEventProvenance.retainSourceRow(b));
    }

    @Test
    void identicalContentStillDistinctProvenanceByOrdinal() {
        TapeRowRef a = row(10, 20, "t", "t", 1L, 2L, 3L, "X", 0);
        TapeRowRef b = row(10, 20, "t", "t", 1L, 2L, 3L, "X", 1);
        assertFalse(DislEventProvenance.contentDiffers(a, b));
        assertNotEquals(DislEventProvenance.provenanceKey(a), DislEventProvenance.provenanceKey(b));
        assertTrue(DislEventProvenance.retainSourceRow(a));
        assertTrue(DislEventProvenance.retainSourceRow(b));
    }

    private static TapeRowRef row(
            long report,
            long wagon,
            String getting,
            String location,
            Long station,
            Long op,
            Long state,
            String train,
            int ordinal) {
        return new TapeRowRef(report, wagon, getting, location, station, op, state, train, ordinal);
    }
}
