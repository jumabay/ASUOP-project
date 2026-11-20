package platform.oracle.migration.l0;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import platform.oracle.migration.l0.model.CurrentStateRecord;
import platform.oracle.migration.l0.model.KitRecord;
import platform.oracle.migration.l0.model.RefRecord;
import platform.oracle.migration.l0.model.RepairRecord;
import platform.oracle.migration.l0.model.StockRecord;
import platform.oracle.migration.l0.model.TapeEventRecord;
import platform.oracle.migration.l0.model.WagonRecord;

/**
 * Synthetic migration fixture pack under {@code fixtures/migration/v1}.
 * Offline only — never opens JDBC / Oracle.
 */
public final class MigrationFixturePack {

    public static final String SCHEMA_VERSION = "migration-l0-v1";

    private final String packId;
    private final LocalDateTime cutoverTs;
    private final LocalDateTime cutoffTs;
    private final String axis;
    private final List<String> gapsOpen;
    private final Map<String, List<RefRecord>> refs;
    private final List<WagonRecord> wagons;
    private final List<CurrentStateRecord> current;
    private final List<RepairRecord> repairs;
    private final List<StockRecord> stock;
    private final List<KitRecord> kits;
    private final List<TapeEventRecord> tape;
    private final JsonNode expectedSelection;
    private final JsonNode expectedReconcile;

    private MigrationFixturePack(
            String packId,
            LocalDateTime cutoverTs,
            LocalDateTime cutoffTs,
            String axis,
            List<String> gapsOpen,
            Map<String, List<RefRecord>> refs,
            List<WagonRecord> wagons,
            List<CurrentStateRecord> current,
            List<RepairRecord> repairs,
            List<StockRecord> stock,
            List<KitRecord> kits,
            List<TapeEventRecord> tape,
            JsonNode expectedSelection,
            JsonNode expectedReconcile) {
        this.packId = packId;
        this.cutoverTs = cutoverTs;
        this.cutoffTs = cutoffTs;
        this.axis = axis;
        this.gapsOpen = List.copyOf(gapsOpen);
        this.refs = copyRefs(refs);
        this.wagons = List.copyOf(wagons);
        this.current = List.copyOf(current);
        this.repairs = List.copyOf(repairs);
        this.stock = List.copyOf(stock);
        this.kits = List.copyOf(kits);
        this.tape = List.copyOf(tape);
        this.expectedSelection = expectedSelection;
        this.expectedReconcile = expectedReconcile;
    }

    public static MigrationFixturePack load(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        ObjectMapper mapper = objectMapper();

        JsonNode manifest = readTree(mapper, directory.resolve("manifest.json"));
        String packId = textOr(manifest, "pack_id", "migration-l0-v1");
        LocalDateTime cutoverTs = LocalDateTime.parse(text(manifest, "cutover_ts"));
        LocalDateTime cutoffTs = manifest.hasNonNull("cutoff_ts")
                ? LocalDateTime.parse(text(manifest, "cutoff_ts"))
                : cutoverTs.minusMonths(12);
        String axis = textOr(manifest, "axis", "GETTING_DATE");
        List<String> gapsOpen = readStringArray(manifest.get("gaps_open"));
        if (gapsOpen.isEmpty()) {
            gapsOpen = OpenGaps.stillOpen();
        }

        Map<String, List<RefRecord>> refs = readRefs(mapper, directory.resolve("refs.json"));
        List<WagonRecord> wagons = readArray(mapper, directory.resolve("wagons.json"), "wagons", WagonRecord.class);
        List<CurrentStateRecord> current =
                readArray(mapper, directory.resolve("current.json"), "current", CurrentStateRecord.class);
        List<RepairRecord> repairs =
                readArray(mapper, directory.resolve("repairs.json"), "repairs", RepairRecord.class);
        List<StockRecord> stock = readArray(mapper, directory.resolve("stock.json"), "stock", StockRecord.class);
        List<KitRecord> kits = readArray(mapper, directory.resolve("kits.json"), "kits", KitRecord.class);
        List<TapeEventRecord> tape =
                readArray(mapper, directory.resolve("tape.json"), "tape", TapeEventRecord.class);

        JsonNode expectedSelection = optionalTree(mapper, directory.resolve("expected_selection.json"));
        JsonNode expectedReconcile = optionalTree(mapper, directory.resolve("expected_reconcile.json"));

        return new MigrationFixturePack(
                packId, cutoverTs, cutoffTs, axis, gapsOpen, refs, wagons, current, repairs, stock, kits, tape,
                expectedSelection, expectedReconcile);
    }

    public String packId() {
        return packId;
    }

    public LocalDateTime cutoverTs() {
        return cutoverTs;
    }

    public LocalDateTime cutoffTs() {
        return cutoffTs;
    }

    public String axis() {
        return axis;
    }

    public List<String> gapsOpen() {
        return gapsOpen;
    }

    public Map<String, List<RefRecord>> refs() {
        return refs;
    }

    public List<WagonRecord> wagons() {
        return wagons;
    }

    public List<CurrentStateRecord> current() {
        return current;
    }

    public List<RepairRecord> repairs() {
        return repairs;
    }

    public List<StockRecord> stock() {
        return stock;
    }

    public List<KitRecord> kits() {
        return kits;
    }

    public List<TapeEventRecord> tape() {
        return tape;
    }

    public JsonNode expectedSelection() {
        return expectedSelection;
    }

    public JsonNode expectedReconcile() {
        return expectedReconcile;
    }

    public static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    private static Map<String, List<RefRecord>> copyRefs(Map<String, List<RefRecord>> refs) {
        Map<String, List<RefRecord>> copy = new LinkedHashMap<>();
        refs.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        return Map.copyOf(copy);
    }

    private static Map<String, List<RefRecord>> readRefs(ObjectMapper mapper, Path path) throws IOException {
        JsonNode root = readTree(mapper, path);
        Map<String, List<RefRecord>> out = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if ("schema_version".equals(entry.getKey())) {
                continue;
            }
            JsonNode value = entry.getValue();
            if (value != null && value.isArray()) {
                List<RefRecord> rows = new ArrayList<>();
                for (JsonNode node : value) {
                    rows.add(mapper.convertValue(node, RefRecord.class));
                }
                out.put(entry.getKey(), rows);
            }
        }
        return out;
    }

    private static JsonNode readTree(ObjectMapper mapper, Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Missing fixture file: " + path);
        }
        try (InputStream in = Files.newInputStream(path)) {
            return mapper.readTree(in);
        }
    }

    private static JsonNode optionalTree(ObjectMapper mapper, Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            return mapper.readTree(in);
        }
    }

    private static <T> List<T> readArray(ObjectMapper mapper, Path path, String field, Class<T> type)
            throws IOException {
        JsonNode root = readTree(mapper, path);
        JsonNode array = root.get(field);
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<T> out = new ArrayList<>();
        for (JsonNode node : array) {
            out.add(mapper.convertValue(node, type));
        }
        return out;
    }

    private static List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonNode item : node) {
            out.add(item.asText());
        }
        return out;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return value.asText();
    }

    private static String textOr(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return value.asText();
    }
}
