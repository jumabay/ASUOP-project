package platform.app.api.dto;

import java.time.LocalDateTime;

public record StateReportLineDto(
        long reportId,
        String wagonCode,
        LocalDateTime dislocationTime,
        String stationCode,
        String operationCode
) {
}
