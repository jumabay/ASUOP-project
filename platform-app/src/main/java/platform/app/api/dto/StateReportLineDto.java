package platform.app.api.dto;

import java.time.LocalDateTime;

/**
 * A1 line DTO. {@code snapshotTime} is the day/slice axis ({@code GETTING_DATE}).
 * {@code dislocationTime} is a deprecated alias and must equal {@code snapshotTime}.
 */
public record StateReportLineDto(
        long reportId,
        String wagonCode,
        LocalDateTime snapshotTime,
        LocalDateTime dislocationTime,
        String stationCode,
        String operationCode
) {
}
