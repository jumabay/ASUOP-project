package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-memory synthetic wagon catalog shared by card/registry/watched adapters.
 *
 * <p>Default (T0): loads {@code synthetic/wagons/catalog.json} (250 codes) for normal CI.
 * Scale catalogs are built on demand via {@link #generated(int, long)} — not committed as JSON.
 *
 * <p>Optional runtime override (manual smoke only):
 * {@code -Dplatform.synthetic.catalog.size=18000}
 * and optional {@code -Dplatform.synthetic.catalog.seed=20261120}.
 * Synthetic in-memory scale ≠ Oracle load proof.
 */
@Component
@Profile("synthetic")
public final class SyntheticCatalog {

    public static final String SIZE_PROPERTY = "platform.synthetic.catalog.size";
    public static final String SEED_PROPERTY = "platform.synthetic.catalog.seed";

    private final List<String> wagonCodes;
    private final Set<String> wagonCodeSet;

    @Autowired
    public SyntheticCatalog(ObjectMapper mapper) {
        this(resolveCodes(mapper));
    }

    SyntheticCatalog(List<String> wagonCodes) {
        SyntheticWagonCodeGenerator.requireSyntheticOnly(wagonCodes);
        this.wagonCodes = List.copyOf(wagonCodes);
        this.wagonCodeSet = Set.copyOf(this.wagonCodes);
    }

    /** Classpath T0 fixture (250). */
    static SyntheticCatalog classpath(ObjectMapper mapper) {
        return new SyntheticCatalog(loadClasspath(mapper));
    }

    /** Seeded generated catalog for scale tests (18k / 30k / T1). */
    static SyntheticCatalog generated(int size, long seed) {
        return new SyntheticCatalog(SyntheticWagonCodeGenerator.generate(size, seed));
    }

    static SyntheticCatalog generated(int size) {
        return generated(size, SyntheticWagonCodeGenerator.DEFAULT_SEED);
    }

    List<String> wagonCodes() {
        return wagonCodes;
    }

    int size() {
        return wagonCodes.size();
    }

    boolean contains(String wagonCode) {
        return wagonCodeSet.contains(wagonCode);
    }

    private static List<String> resolveCodes(ObjectMapper mapper) {
        String sizeProp = System.getProperty(SIZE_PROPERTY);
        if (sizeProp != null && !sizeProp.isBlank()) {
            int size = Integer.parseInt(sizeProp.trim());
            long seed = Long.getLong(SEED_PROPERTY, SyntheticWagonCodeGenerator.DEFAULT_SEED);
            return SyntheticWagonCodeGenerator.generate(size, seed);
        }
        return loadClasspath(mapper);
    }

    private static List<String> loadClasspath(ObjectMapper mapper) {
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
            List<String> codes = new ArrayList<>(unique);
            SyntheticWagonCodeGenerator.requireSyntheticOnly(codes);
            return codes;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read synthetic wagon catalog", e);
        }
    }
}
