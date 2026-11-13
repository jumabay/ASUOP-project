package platform.disl.journal;

import java.util.Objects;

/** One registered non-empty kit in the local hour journal. */
public final class SliceKit {

    private final SliceKitKey key;
    private final ChannelId channelId;
    private final HourSlot hourSlot;
    private final int lineCount;

    public SliceKit(SliceKitKey key, ChannelId channelId, HourSlot hourSlot, int lineCount) {
        this.key = Objects.requireNonNull(key, "key");
        this.channelId = Objects.requireNonNull(channelId, "channelId");
        this.hourSlot = Objects.requireNonNull(hourSlot, "hourSlot");
        if (lineCount < 0) {
            throw new IllegalArgumentException("lineCount must be >= 0");
        }
        this.lineCount = lineCount;
    }

    public SliceKitKey key() {
        return key;
    }

    public ChannelId channelId() {
        return channelId;
    }

    public HourSlot hourSlot() {
        return hourSlot;
    }

    public int lineCount() {
        return lineCount;
    }
}
