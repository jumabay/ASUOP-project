package platform.dislocation;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class DayStateReportQuery {

    public static final int MAX_LINES = 500;

    private final StateReportDayPort port;

    public DayStateReportQuery(StateReportDayPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public StateReportDay getForDay(String wagonCode, LocalDate date) {
        String code = normalizeCode(wagonCode);
        LocalDate day = Objects.requireNonNull(date, "date");
        List<StateReportLine> lines = port.findLinesForDay(code, day);
        if (lines.size() > MAX_LINES) {
            throw new ResultLimitExceededException(code, day, lines.size(), MAX_LINES);
        }
        return new StateReportDay(code, day, lines);
    }

    static String normalizeCode(String wagonCode) {
        if (wagonCode == null || wagonCode.isBlank()) {
            throw new InvalidWagonCodeException("Wagon code must not be blank");
        }
        return wagonCode.trim();
    }
}
