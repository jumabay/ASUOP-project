package platform.dislocation;

import java.time.LocalDateTime;

public record StateReportLineKey(
        long reportId,
        String wagonCode,
        LocalDateTime dislocationTime,
        String stationCode,
        String operationCode
) {
}
