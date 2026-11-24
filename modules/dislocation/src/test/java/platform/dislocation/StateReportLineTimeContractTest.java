package platform.dislocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StateReportLineTimeContractTest {

    @Test
    void dislocationTimeAliasEqualsSnapshotTime() {
        LocalDateTime t = LocalDateTime.of(2024, 1, 15, 8, 0, 0);
        StateReportLine line = new StateReportLine(1L, "SYN-00001", t, "6900", "01");
        assertEquals(t, line.snapshotTime());
        assertEquals(line.snapshotTime(), line.dislocationTime());
    }

    @Test
    void twoLinesMayShareSameSnapshotTime() {
        LocalDateTime t = LocalDateTime.of(2024, 1, 15, 8, 0, 0);
        StateReportLine a = new StateReportLine(1L, "SYN-00001", t, "6900", "01");
        StateReportLine b = new StateReportLine(2L, "SYN-00001", t, "6901", "02");
        assertEquals(a.snapshotTime(), b.snapshotTime());
        assertNotEquals(a.reportId(), b.reportId());
        assertNotEquals(a.key(), b.key());
    }
}
