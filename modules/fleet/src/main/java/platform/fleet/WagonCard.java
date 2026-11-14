package platform.fleet;

import java.util.Objects;

/**
 * A2 wagon card fields (passport subset).
 */
public record WagonCard(
        String wagonCode,
        String model,
        String wagonKind,
        String ownerName,
        String homeStationCode,
        String homeStationName,
        boolean inventoryPark
) {
    public WagonCard {
        Objects.requireNonNull(wagonCode, "wagonCode");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(wagonKind, "wagonKind");
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(homeStationCode, "homeStationCode");
        Objects.requireNonNull(homeStationName, "homeStationName");
    }
}
