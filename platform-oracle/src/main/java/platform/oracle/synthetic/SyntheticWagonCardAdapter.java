package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import platform.fleet.WagonCard;
import platform.fleet.WagonCardPort;
import platform.fleet.WagonCurrentState;

@Component
@Profile("synthetic")
public class SyntheticWagonCardAdapter implements WagonCardPort {

    private final ObjectMapper mapper;
    private final ClassLoader classLoader;
    private final SyntheticCatalog catalog;

    @Autowired
    public SyntheticWagonCardAdapter(ObjectMapper mapper, SyntheticCatalog catalog) {
        this.mapper = mapper;
        this.classLoader = SyntheticWagonCardAdapter.class.getClassLoader();
        this.catalog = catalog;
    }

    /** Test / non-Spring helper: builds T0 classpath catalog. */
    SyntheticWagonCardAdapter(ObjectMapper mapper) {
        this(mapper, SyntheticCatalog.classpath(mapper));
    }

    @Override
    public Optional<WagonCard> findByCode(String wagonCode) {
        WagonCard passport = loadPassport(wagonCode);
        if (passport == null) {
            return Optional.empty();
        }
        return Optional.of(new WagonCard(
                passport.wagonCode(),
                passport.model(),
                passport.wagonKind(),
                passport.ownerName(),
                passport.homeStationCode(),
                passport.homeStationName(),
                passport.inventoryPark(),
                loadCurrent(wagonCode)));
    }

    private WagonCard loadPassport(String wagonCode) {
        String path = "synthetic/wagons/" + wagonCode + ".card.json";
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in != null) {
                JsonNode node = mapper.readTree(in);
                return new WagonCard(
                        node.get("wagonCode").asText(),
                        node.get("model").asText(),
                        node.get("wagonKind").asText(),
                        node.get("ownerName").asText(),
                        node.get("homeStationCode").asText(),
                        node.get("homeStationName").asText(),
                        node.get("inventoryPark").asBoolean(),
                        null);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read synthetic wagon card: " + wagonCode, e);
        }

        if (!catalog.contains(wagonCode)) {
            return null;
        }
        return stubCard(wagonCode);
    }

    /**
     * Separate current-state fixture ({@code *.current.json}); not derived from day history.
     * Missing file → null current.
     */
    private WagonCurrentState loadCurrent(String wagonCode) {
        String path = "synthetic/wagons/" + wagonCode + ".current.json";
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            JsonNode node = mapper.readTree(in);
            return new WagonCurrentState(
                    textOrNull(node, "stationCode"),
                    textOrNull(node, "operationCode"),
                    textOrNull(node, "dislocationTime"),
                    textOrNull(node, "trainIndex"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read synthetic wagon current: " + wagonCode, e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    static WagonCard stubCard(String wagonCode) {
        return new WagonCard(
                wagonCode,
                "12-132",
                "КРЫТЫЙ",
                "SYN-OWNER",
                "6900",
                "SYN-STATION",
                true,
                null);
    }
}
