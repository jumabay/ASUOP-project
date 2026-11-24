package platform.fleet;

import java.util.Objects;

/**
 * Registry row with passport fields (P5-C additive enrichment of R2).
 */
public record WagonRegistryItem(
        String wagonCode,
        String model,
        String wagonKind,
        String ownerName,
        String homeStationCode,
        String homeStationName,
        /** Null = unknown. */
        Boolean inventoryPark
) {
    public WagonRegistryItem {
        Objects.requireNonNull(wagonCode, "wagonCode");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(wagonKind, "wagonKind");
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(homeStationCode, "homeStationCode");
        Objects.requireNonNull(homeStationName, "homeStationName");
    }

    /** Passport-only mapping; ignores {@link WagonCard#currentState()}. */
    public static WagonRegistryItem fromCard(WagonCard card) {
        return new WagonRegistryItem(
                card.wagonCode(),
                card.model(),
                card.wagonKind(),
                card.ownerName(),
                card.homeStationCode(),
                card.homeStationName(),
                card.inventoryPark());
    }
}
