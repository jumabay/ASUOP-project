package platform.dislocation;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record StateReportDay(
        String wagonCode,
        LocalDate date,
        List<StateReportLine> lines
) {
    public StateReportDay {
        Objects.requireNonNull(wagonCode, "wagonCode");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
    }
}
