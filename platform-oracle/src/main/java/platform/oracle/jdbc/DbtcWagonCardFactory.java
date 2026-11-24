package platform.oracle.jdbc;

import java.math.BigDecimal;
import platform.fleet.WagonCard;
import platform.fleet.WagonCurrentState;

/**
 * Builds {@link WagonCard} from DBTC columns. Inventory mapping: only raw {@code 1} →
 * {@code true} is CONFIRMED for CONTROL; other values (including {@code 0} and null) stay
 * unknown ({@code null}) until a proven legacy rule exists.
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
            String ownerDisplay,
            WagonCurrentState currentState) {
        return new WagonCard(
                wagonCode,
                nullToEmpty(modelDisplay),
                nullToEmpty(kindDisplay),
                nullToEmpty(ownerDisplay),
                nullToEmpty(homeStationCode),
                nullToEmpty(homeStationName),
                toInventoryBoolean(inventoryParkRaw),
                currentState);
    }

    /** Without ref SELECT: only code + inventory raw; other passport empty; no current. */
    public static WagonCard coreOnly(String wagonCode, Number inventoryParkRaw) {
        return fromCoreRow(wagonCode, "", "", inventoryParkRaw, "", "", "", null);
    }

    /**
     * Confirmed: {@code 1} → {@code true}. Null and any other numeric value → {@code null}
     * (unknown). Does not treat {@code raw != 0} as true.
     */
    static Boolean toInventoryBoolean(Number raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal bd) {
            if (bd.compareTo(BigDecimal.ONE) == 0) {
                return Boolean.TRUE;
            }
            return null;
        }
        if (raw instanceof Float || raw instanceof Double) {
            double d = raw.doubleValue();
            if (d == 1.0d) {
                return Boolean.TRUE;
            }
            return null;
        }
        if (raw.longValue() == 1L) {
            return Boolean.TRUE;
        }
        return null;
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
