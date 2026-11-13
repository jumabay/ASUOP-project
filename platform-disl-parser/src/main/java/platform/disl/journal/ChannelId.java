package platform.disl.journal;

import java.util.Objects;

/** Logical import channel (e.g. KZ). Separate from numeric slice type. */
public final class ChannelId {

    private final String value;

    public ChannelId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("channel id must be non-blank");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelId that)) {
            return false;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
