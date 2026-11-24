package platform.dislocation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One A1 day-history line. Time axis is {@link #snapshotTime()} ({@code GETTING_DATE} /
 * kit slice). {@link #dislocationTime()} is a deprecated alias of the same value (T1).
 */
public record StateReportLine(
        long reportId,
        String wagonCode,
        LocalDateTime snapshotTime,
        String stationCode,
        String operationCode
) {
    public StateReportLine {
        Objects.requireNonNull(wagonCode, "wagonCode");
        Objects.requireNonNull(snapshotTime, "snapshotTime");
        Objects.requireNonNull(stationCode, "stationCode");
        Objects.requireNonNull(operationCode, "operationCode");
    }

    /**
     * Deprecated alias of {@link #snapshotTime()}. Always equal; never LOCATION_DATE.
     */
    public LocalDateTime dislocationTime() {
        return snapshotTime;
    }

    public StateReportLineKey key() {
        return new StateReportLineKey(reportId, wagonCode, snapshotTime, stationCode, operationCode);
    }
}
