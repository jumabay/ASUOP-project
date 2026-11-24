package platform.fleet;

import java.util.Objects;

/**
 * A2 wagon card fields (passport subset) plus optional current-state block (P5-C).
 */
public record WagonCard(
        String wagonCode,
        String model,
        String wagonKind,
        String ownerName,
        String homeStationCode,
        String homeStationName,
        boolean inventoryPark,
        WagonCurrentState currentState
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
