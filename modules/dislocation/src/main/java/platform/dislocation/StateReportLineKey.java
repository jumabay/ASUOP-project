package platform.dislocation;

import java.time.LocalDateTime;

public record StateReportLineKey(
        long reportId,
        String wagonCode,
        LocalDateTime snapshotTime,
        String stationCode,
        String operationCode
) {
}
