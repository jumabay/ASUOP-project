package platform.oracle.synthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.stereotype.Component;
import platform.fleet.WagonCard;
import platform.fleet.WagonCardPort;

@Component
public class SyntheticWagonCardAdapter implements WagonCardPort {

    private final ObjectMapper mapper;
    private final ClassLoader classLoader;

    public SyntheticWagonCardAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
        this.classLoader = SyntheticWagonCardAdapter.class.getClassLoader();
    }

    @Override
    public Optional<WagonCard> findByCode(String wagonCode) {
        String path = "synthetic/wagons/" + wagonCode + ".card.json";
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in == null) {
                return Optional.empty();
            }
            JsonNode node = mapper.readTree(in);
            return Optional.of(new WagonCard(
                    node.get("wagonCode").asText(),
                    node.get("model").asText(),
                    node.get("wagonKind").asText(),
                    node.get("ownerName").asText(),
                    node.get("homeStationCode").asText(),
                    node.get("homeStationName").asText(),
                    node.get("inventoryPark").asBoolean()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read synthetic wagon card: " + wagonCode, e);
        }
    }
}
