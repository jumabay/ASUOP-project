package platform.disl.journal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory local kit journal. Independent of Oracle.
 * Key {@code (fileName, type)} is unique: a repeat does not add a second kit.
 */
public final class LocalHourJournal {

    private final Map<SliceKitKey, SliceKit> kits = new LinkedHashMap<>();

    /**
     * Registers a kit. Returns empty if the same name+type already exists.
     */
    public Optional<SliceKit> tryRegister(SliceKit kit) {
        Objects.requireNonNull(kit, "kit");
        if (kits.containsKey(kit.key())) {
            return Optional.empty();
        }
        kits.put(kit.key(), kit);
        return Optional.of(kit);
    }

    public boolean contains(SliceKitKey key) {
        return kits.containsKey(Objects.requireNonNull(key, "key"));
    }

    public Optional<SliceKit> find(SliceKitKey key) {
        return Optional.ofNullable(kits.get(Objects.requireNonNull(key, "key")));
    }

    public int size() {
        return kits.size();
    }

    public Collection<SliceKit> all() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public List<SliceKit> kitsForChannel(ChannelId channelId) {
        Objects.requireNonNull(channelId, "channelId");
        List<SliceKit> result = new ArrayList<>();
        for (SliceKit kit : kits.values()) {
            if (kit.channelId().equals(channelId)) {
                result.add(kit);
            }
        }
        return List.copyOf(result);
    }

    public boolean hasKitFor(ChannelId channelId, HourSlot hourSlot) {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(hourSlot, "hourSlot");
        for (SliceKit kit : kits.values()) {
            if (kit.channelId().equals(channelId) && kit.hourSlot().equals(hourSlot)) {
                return true;
            }
        }
        return false;
    }
}
