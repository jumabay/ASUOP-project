package platform.oracle.jdbc;

import java.time.LocalDateTime;
import platform.dislocation.StateReportLine;

/**
 * A1 line factory. {@code dislocationTime} is taken from GETTING_DATE (day axis) —
 * PROVISIONAL, not verified as factual LOCATION_DATE (DT1 OPEN).
 * Never put numeric station/operation ids into code fields.
 */
public final class DbtcStateReportLineFactory {

    private DbtcStateReportLineFactory() {}

    public static StateReportLine fromGettingAxis(
            long reportId,
            String wagonCode,
            LocalDateTime gettingDate,
            String stationCode,
            String operationCode) {
        if (gettingDate == null) {
            throw new IllegalArgumentException("gettingDate required for provisional dislocationTime");
        }
        return new StateReportLine(
                reportId,
                wagonCode,
                gettingDate,
                nullToEmpty(stationCode),
                nullToEmpty(operationCode));
    }

    /** Without STATIONS/OPERATIONS SELECT: codes empty; ids discarded. */
    public static StateReportLine coreOnly(
            long reportId, String wagonCode, LocalDateTime gettingDate) {
        return fromGettingAxis(reportId, wagonCode, gettingDate, "", "");
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
