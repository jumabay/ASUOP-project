package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import platform.dislocation.DayInterval;
import platform.dislocation.StateReportDayPort;
import platform.dislocation.StateReportLine;

@Component
@Profile("synthetic")
public class SyntheticStateReportDayAdapter implements StateReportDayPort {

    private final ObjectMapper mapper;
    private final ClassLoader classLoader;
    private final Map<String, List<StateReportLine>> overlays;

    @Autowired
    public SyntheticStateReportDayAdapter(ObjectMapper mapper) {
        this(mapper, Map.of());
    }

    /**
     * Test/helper: optional in-memory overlays keyed by {@code wagonCode|date}
     * (see {@link #overlayKey(String, LocalDate)}). Used for T3 over-limit without
     * committing a 501-line JSON blob.
     */
    public SyntheticStateReportDayAdapter(ObjectMapper mapper, Map<String, List<StateReportLine>> overlays) {
        this.mapper = mapper;
        this.classLoader = SyntheticStateReportDayAdapter.class.getClassLoader();
        this.overlays = Map.copyOf(Objects.requireNonNull(overlays, "overlays"));
    }

    public static String overlayKey(String wagonCode, LocalDate date) {
        return wagonCode + "|" + date;
    }

    @Override
    public List<StateReportLine> findLinesForDay(String wagonCode, LocalDate date) {
        List<StateReportLine> overlay = overlays.get(overlayKey(wagonCode, date));
        if (overlay != null) {
            return overlay;
        }

        DayInterval interval = DayInterval.of(date);
        String path = "synthetic/state-reports/" + wagonCode + "." + date.toString().replace("-", "") + ".json";
        String emptyPath = "synthetic/state-reports/" + wagonCode + "." + date.toString().replace("-", "") + ".empty.json";

        JsonNode root = readOptional(path);
        if (root == null) {
            root = readOptional(emptyPath);
        }
        if (root == null) {
            return List.of();
        }

        List<StateReportLine> result = new ArrayList<>();
        for (JsonNode line : root.withArray("lines")) {
            // Prefer snapshotTime; fixtures may still carry dislocationTime as the line clock.
            LocalDateTime time = parseSnapshotTime(line);
            if (!interval.contains(time)) {
                continue;
            }
            result.add(new StateReportLine(
                    line.get("reportId").asLong(),
                    line.get("wagonCode").asText(),
                    time,
                    line.get("stationCode").asText(),
                    line.get("operationCode").asText()));
        }
        return List.copyOf(result);
    }

    static LocalDateTime parseSnapshotTime(JsonNode line) {
        if (line.hasNonNull("snapshotTime") && !line.get("snapshotTime").asText().isBlank()) {
            return LocalDateTime.parse(line.get("snapshotTime").asText());
        }
        return LocalDateTime.parse(line.get("dislocationTime").asText());
    }

    private JsonNode readOptional(String path) {
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read synthetic state reports: " + path, e);
        }
    }
}
