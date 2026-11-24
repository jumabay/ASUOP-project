package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Synthetic watch-list membership: codes only from classpath fixture.
 * Labeled {@link platform.fleet.WatchedMembershipSource#FIXTURE} by the adapter —
 * not confirmed as Oracle GetActiveWagonList (G31 OPEN).
 */
final class SyntheticWatchedList {

    private final List<String> wagonCodes;
    private final Set<String> wagonCodeSet;

    SyntheticWatchedList(ObjectMapper mapper, SyntheticCatalog catalog) {
        List<String> loaded = load(mapper);
        for (String code : loaded) {
            if (!catalog.contains(code)) {
                throw new IllegalStateException(
                        "Watched fixture code missing from catalog: " + code);
            }
        }
        this.wagonCodes = List.copyOf(loaded);
        this.wagonCodeSet = Set.copyOf(wagonCodes);
    }

    List<String> wagonCodes() {
        return wagonCodes;
    }

    boolean contains(String wagonCode) {
        return wagonCodeSet.contains(wagonCode);
    }

    private static List<String> load(ObjectMapper mapper) {
        try (InputStream in = SyntheticWatchedList.class.getClassLoader()
                .getResourceAsStream("synthetic/wagons/watched.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing synthetic/wagons/watched.json");
            }
            JsonNode root = mapper.readTree(in);
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (JsonNode node : root.withArray("wagons")) {
                unique.add(node.asText());
            }
            return new ArrayList<>(unique);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read synthetic watched wagon list", e);
        }
    }
}
