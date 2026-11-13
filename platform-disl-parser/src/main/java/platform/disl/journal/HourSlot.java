package platform.disl.journal;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Local hour slot for the kit journal and miss calendar.
 * Always normalized to the start of the hour; no timezone conversion in R0.
 */
public final class HourSlot implements Comparable<HourSlot> {

    private final LocalDateTime hourStart;

    private HourSlot(LocalDateTime hourStart) {
        this.hourStart = hourStart;
    }

    public static HourSlot of(LocalDateTime value) {
        Objects.requireNonNull(value, "value");
        return new HourSlot(value.withMinute(0).withSecond(0).withNano(0));
    }

    public LocalDateTime hourStart() {
        return hourStart;
    }

    public HourSlot plusHours(long hours) {
        return new HourSlot(hourStart.plusHours(hours));
    }

    @Override
    public int compareTo(HourSlot other) {
        return hourStart.compareTo(other.hourStart);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HourSlot that)) {
            return false;
        }
        return hourStart.equals(that.hourStart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hourStart);
    }

    @Override
    public String toString() {
        return hourStart.toString();
    }
}
