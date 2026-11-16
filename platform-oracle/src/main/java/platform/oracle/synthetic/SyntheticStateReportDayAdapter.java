package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    public SyntheticStateReportDayAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
        this.classLoader = SyntheticStateReportDayAdapter.class.getClassLoader();
    }

    @Override
    public List<StateReportLine> findLinesForDay(String wagonCode, LocalDate date) {
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
            LocalDateTime time = LocalDateTime.parse(line.get("dislocationTime").asText());
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
