package platform.oracle.migration.l0;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import platform.oracle.migration.l0.model.KitRecord;

/**
 * Business UK for import kits: {@code (filename, type)} → one header on replay.
 * Physical tape rows are NOT collapsed (NK1 OPEN).
 */
public final class KitIdempotencyIndex {

    public record KitKey(String filename, int type) {
        public KitKey {
            Objects.requireNonNull(filename, "filename");
        }
    }

    private final Map<KitKey, KitRecord> headers = new LinkedHashMap<>();

    /**
     * @return previously retained header if this insert was a replay collision; otherwise {@code null}
     */
    public KitRecord put(KitRecord kit) {
        Objects.requireNonNull(kit, "kit");
        KitKey key = new KitKey(kit.filename(), kit.type());
        return headers.putIfAbsent(key, kit);
    }

    public int size() {
        return headers.size();
    }

    public KitRecord get(String filename, int type) {
        return headers.get(new KitKey(filename, type));
    }

    public Map<KitKey, KitRecord> snapshot() {
        return Map.copyOf(headers);
    }
}
