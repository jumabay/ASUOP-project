package platform.oracle.jdbc;

import platform.fleet.WagonCard;

/**
 * Builds {@link WagonCard} from raw DBTC columns without inventing ref codes.
 * Missing/unknown display fields → empty string (API contract non-null); inventory default false.
 */
public final class DbtcWagonCardFactory {

    private DbtcWagonCardFactory() {}

    public static WagonCard fromCoreRow(
            String wagonCode,
            String homeStationCode,
            String homeStationName,
            Number inventoryParkRaw,
            String modelDisplay,
            String kindDisplay,
            String ownerDisplay) {
        return new WagonCard(
                wagonCode,
                nullToEmpty(modelDisplay),
                nullToEmpty(kindDisplay),
                nullToEmpty(ownerDisplay),
                nullToEmpty(homeStationCode),
                nullToEmpty(homeStationName),
                toInventoryBoolean(inventoryParkRaw),
                null // currentState out of P5-E smoke
        );
    }

    /** Without ref SELECT: only code + inventory raw; other passport empty. */
    public static WagonCard coreOnly(String wagonCode, Number inventoryParkRaw) {
        return fromCoreRow(wagonCode, "", "", inventoryParkRaw, "", "", "");
    }

    static boolean toInventoryBoolean(Number raw) {
        if (raw == null) {
            return false;
        }
        return raw.longValue() != 0L;
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
