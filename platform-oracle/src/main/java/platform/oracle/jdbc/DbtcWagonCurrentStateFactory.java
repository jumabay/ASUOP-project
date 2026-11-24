package platform.oracle.jdbc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import platform.fleet.WagonCurrentState;

/**
 * Builds optional card {@link WagonCurrentState} from WAGON master columns (P5-E-CUR).
 *
 * <p>Not derived from day tape. {@code trainIndex} stays null until a separate mapping
 * approval (TRAIN column discovery alone is insufficient).
 */
public final class DbtcWagonCurrentStateFactory {

    private DbtcWagonCurrentStateFactory() {}

    /**
     * @param locationStationCode soft-joined {@code STATIONS.CODE} (may be null/blank)
     * @param operationIdCode {@code OPERATIONS.IDCODE} via {@link java.sql.ResultSet#getString}
     * @param operationCodeRaw {@code OPERATIONS.CODE} string fallback
     * @param locationDate {@code WAGON.LOCATIONDATE} only — never GETTING_DATE / SYSDATE / REPORTDATE
     */
    public static WagonCurrentState fromMasterRow(
            String locationStationCode,
            String operationIdCode,
            String operationCodeRaw,
            Timestamp locationDate) {
        String station = blankToNull(locationStationCode);
        String operation = blankToNull(DbtcOperationCode.resolve(operationIdCode, operationCodeRaw));
        String dislocationTime = formatLocationDate(locationDate);
        // trainIndex: always null under E-CUR (no mapping approval for TRAIN*).
        String trainIndex = null;

        if (station == null && operation == null && dislocationTime == null && trainIndex == null) {
            return null;
        }
        return new WagonCurrentState(station, operation, dislocationTime, trainIndex);
    }

    /** Naive local clock string — no ZoneId / Instant conversion. */
    static String formatLocationDate(Timestamp locationDate) {
        if (locationDate == null) {
            return null;
        }
        LocalDateTime ldt = locationDate.toLocalDateTime();
        return ldt.toString();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
