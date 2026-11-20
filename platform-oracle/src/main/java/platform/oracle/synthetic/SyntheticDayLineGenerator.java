package platform.oracle.synthetic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import platform.dislocation.StateReportLine;

/**
 * In-memory day-history line generator for over-limit boundary tests (T3).
 * Does not claim Oracle tape semantics.
 */
public final class SyntheticDayLineGenerator {

    private SyntheticDayLineGenerator() {
    }

    /**
     * Builds {@code count} lines inside the calendar day (half-open interval start).
     * Times are spaced by one second from 00:00:00; for count &gt; 86 400 the hour wraps
     * via plusSeconds (still same LocalDate for typical T3 size 501).
     */
    public static List<StateReportLine> generate(String wagonCode, LocalDate date, int count) {
        Objects.requireNonNull(wagonCode, "wagonCode");
        Objects.requireNonNull(date, "date");
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1, got " + count);
        }
        List<StateReportLine> lines = new ArrayList<>(count);
        LocalDateTime base = date.atStartOfDay();
        for (int i = 0; i < count; i++) {
            lines.add(new StateReportLine(
                    10_000L + i,
                    wagonCode,
                    base.plusSeconds(i),
                    "6900",
                    "01"));
        }
        return List.copyOf(lines);
    }
}
