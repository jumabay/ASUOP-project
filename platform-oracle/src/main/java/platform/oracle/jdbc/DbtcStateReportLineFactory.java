package platform.oracle.jdbc;

import java.time.LocalDateTime;
import platform.dislocation.StateReportLine;

/**
 * A1 line factory. {@code snapshotTime} (and deprecated alias {@code dislocationTime})
 * come from {@code GETTING_DATE} (day/slice axis). Not factual {@code LOCATION_DATE}
 * (DT1 / T2 still OPEN).
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
            throw new IllegalArgumentException("gettingDate required for snapshotTime");
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
