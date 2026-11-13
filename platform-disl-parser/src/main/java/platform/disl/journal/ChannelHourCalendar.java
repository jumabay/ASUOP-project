package platform.disl.journal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Missed-hour calendar for one channel.
 * Uses only explicitly supplied expected slots / range; never reads system time.
 */
public final class ChannelHourCalendar {

    private final LocalHourJournal journal;

    public ChannelHourCalendar(LocalHourJournal journal) {
        this.journal = Objects.requireNonNull(journal, "journal");
    }

    /**
     * Returns expected slots that have no registered kit on the channel, in chronological order.
     */
    public List<HourSlot> findMissing(ChannelId channelId, Collection<HourSlot> expectedSlots) {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(expectedSlots, "expectedSlots");

        TreeSet<HourSlot> ordered = new TreeSet<>();
        for (HourSlot slot : expectedSlots) {
            if (slot != null) {
                ordered.add(slot);
            }
        }

        List<HourSlot> missing = new ArrayList<>();
        for (HourSlot slot : ordered) {
            if (!journal.hasKitFor(channelId, slot)) {
                missing.add(slot);
            }
        }
        return List.copyOf(missing);
    }

    /**
     * Expands an inclusive local-hour range into slots, then lists missing ones for the channel.
     */
    public List<HourSlot> findMissingInRange(ChannelId channelId, HourSlot fromInclusive, HourSlot toInclusive) {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(fromInclusive, "fromInclusive");
        Objects.requireNonNull(toInclusive, "toInclusive");
        if (toInclusive.compareTo(fromInclusive) < 0) {
            throw new IllegalArgumentException("toInclusive must be >= fromInclusive");
        }
        return findMissing(channelId, expandRange(fromInclusive, toInclusive));
    }

    public static List<HourSlot> expandRange(HourSlot fromInclusive, HourSlot toInclusive) {
        Objects.requireNonNull(fromInclusive, "fromInclusive");
        Objects.requireNonNull(toInclusive, "toInclusive");
        if (toInclusive.compareTo(fromInclusive) < 0) {
            throw new IllegalArgumentException("toInclusive must be >= fromInclusive");
        }
        List<HourSlot> slots = new ArrayList<>();
        HourSlot current = fromInclusive;
        while (current.compareTo(toInclusive) <= 0) {
            slots.add(current);
            current = current.plusHours(1);
        }
        return List.copyOf(slots);
    }
}
