package platform.oracle.migration.l0;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Day window for tape/reporting aggregates.
 *
 * <p>Half-open {@code [D 00:00, D+1 00:00)} on the GETTING / {@code dislocation_ts} axis.
 * Timezone resolution is {@code R1-Q-TZ OPEN}: naive wall-clock timestamps until that gap closes.
 * DT1 remains BLOCKED — filter axis stays GETTING, not LOCATION.
 */
public final class DayBoundaryPolicy {

    public static final String DOCUMENTATION =
            "half-open [D 00:00, D+1 00:00) on getting/dislocation_ts; R1-Q-TZ OPEN (naive); DT1 BLOCKED";

    private DayBoundaryPolicy() {
    }

    /** Inclusive start of day D (local/naive). */
    public static LocalDateTime startInclusive(LocalDate day) {
        return day.atStartOfDay();
    }

    /** Exclusive end of day D (start of D+1). */
    public static LocalDateTime endExclusive(LocalDate day) {
        return day.plusDays(1).atStartOfDay();
    }

    public static boolean contains(LocalDate day, LocalDateTime ts) {
        if (ts == null) {
            return false;
        }
        return !ts.isBefore(startInclusive(day)) && ts.isBefore(endExclusive(day));
    }
}
