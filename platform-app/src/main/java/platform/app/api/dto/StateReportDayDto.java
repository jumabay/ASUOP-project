package platform.app.api.dto;

import java.time.LocalDate;
import java.util.List;

public record StateReportDayDto(
        String wagonCode,
        LocalDate date,
        List<StateReportLineDto> lines
) {
}
