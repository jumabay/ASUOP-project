package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SyntheticCatalog {

    private final List<String> wagonCodes;
    private final Set<String> wagonCodeSet;

    SyntheticCatalog(ObjectMapper mapper) {
        this.wagonCodes = List.copyOf(load(mapper));
        this.wagonCodeSet = Set.copyOf(wagonCodes);
    }

    List<String> wagonCodes() {
        return wagonCodes;
    }

    boolean contains(String wagonCode) {
        return wagonCodeSet.contains(wagonCode);
    }

    private static List<String> load(ObjectMapper mapper) {
        try (InputStream in = SyntheticCatalog.class.getClassLoader()
                .getResourceAsStream("synthetic/wagons/catalog.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing synthetic/wagons/catalog.json");
            }
            JsonNode root = mapper.readTree(in);
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (JsonNode node : root.withArray("wagons")) {
                unique.add(node.asText());
            }
            return new ArrayList<>(unique);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read synthetic wagon catalog", e);
        }
    }
}
