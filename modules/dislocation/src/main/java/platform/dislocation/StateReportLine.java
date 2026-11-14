package platform.dislocation;

import java.time.LocalDateTime;
import java.util.Objects;

public record StateReportLine(
        long reportId,
        String wagonCode,
        LocalDateTime dislocationTime,
        String stationCode,
        String operationCode
) {
    public StateReportLine {
        Objects.requireNonNull(wagonCode, "wagonCode");
        Objects.requireNonNull(dislocationTime, "dislocationTime");
        Objects.requireNonNull(stationCode, "stationCode");
        Objects.requireNonNull(operationCode, "operationCode");
    }

    public StateReportLineKey key() {
        return new StateReportLineKey(reportId, wagonCode, dislocationTime, stationCode, operationCode);
    }
}
