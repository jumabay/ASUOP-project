package platform.dislocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Half-open calendar day: {@code [date 00:00:00, date+1 00:00:00)}.
 * Time zone remains open question R1-Q-TZ; uses naive {@link LocalDateTime}.
 */
public final class DayInterval {

    private final LocalDateTime beginInclusive;
    private final LocalDateTime endExclusive;

    private DayInterval(LocalDateTime beginInclusive, LocalDateTime endExclusive) {
        this.beginInclusive = beginInclusive;
        this.endExclusive = endExclusive;
    }

    public static DayInterval of(LocalDate date) {
        Objects.requireNonNull(date, "date");
        LocalDateTime begin = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return new DayInterval(begin, end);
    }

    public LocalDateTime beginInclusive() {
        return beginInclusive;
    }

    public LocalDateTime endExclusive() {
        return endExclusive;
    }

    public boolean contains(LocalDateTime instant) {
        Objects.requireNonNull(instant, "instant");
        return !instant.isBefore(beginInclusive) && instant.isBefore(endExclusive);
    }
}
