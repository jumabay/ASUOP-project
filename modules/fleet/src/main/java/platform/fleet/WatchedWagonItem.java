package platform.fleet;

import java.util.Objects;

/**
 * Watched-list row: A2 passport fields for a wagon on the synthetic watch list.
 */
public record WatchedWagonItem(
        String wagonCode,
        String model,
        String wagonKind,
        String ownerName,
        String homeStationCode,
        String homeStationName,
        /** Null = unknown. */
        Boolean inventoryPark
) {
    public WatchedWagonItem {
        Objects.requireNonNull(wagonCode, "wagonCode");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(wagonKind, "wagonKind");
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(homeStationCode, "homeStationCode");
        Objects.requireNonNull(homeStationName, "homeStationName");
    }

    public static WatchedWagonItem fromCard(WagonCard card) {
        return new WatchedWagonItem(
                card.wagonCode(),
                card.model(),
                card.wagonKind(),
                card.ownerName(),
                card.homeStationCode(),
                card.homeStationName(),
                card.inventoryPark());
    }
}
