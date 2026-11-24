package platform.oracle.jdbc;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import platform.dislocation.StateReportDayPort;
import platform.dislocation.StateReportLine;

/**
 * A1 day lines: filter on GETTING_DATE (slice axis). Maps dislocationTime from getting_date
 * (PROVISIONAL — UI labels as «Время среза», not verified fact LOCATION_DATE).
 * operationCode from OPERATIONS.IDCODE to preserve leading zeros; stationCode from STATIONS.CODE.
 */
@Component
@Profile("dbtc-ro")
public class DbtcJdbcStateReportDayAdapter implements StateReportDayPort {

    private static final String RESOLVE = """
            SELECT id FROM dbtc.wagon WHERE code = :wagon_code FETCH FIRST 1 ROW ONLY
            """;

    private static final String DAY = """
            SELECT r.report_id AS report_id,
                   :wagon_code AS wagon_code,
                   r.getting_date AS getting_date,
                   NVL(st.code, '') AS station_code,
                   NVL(NVL(op.idcode, op.code), '') AS operation_code
            FROM dbtc.statereportrecords r
            LEFT JOIN dbtc.stations st ON st.id = r.location_station_id
            LEFT JOIN dbtc.operations op ON op.id = r.operation_id
            WHERE r.wagon_id = :wagon_id
              AND r.getting_date >= :day_from
              AND r.getting_date < :day_to
            ORDER BY r.getting_date ASC
            FETCH FIRST :fetch_limit ROWS ONLY
            """;

    private final JdbcClient jdbc;

    public DbtcJdbcStateReportDayAdapter(JdbcClient dbtcRoJdbcClient) {
        this.jdbc = dbtcRoJdbcClient;
    }

    @Override
    public List<StateReportLine> findLinesForDay(String wagonCode, LocalDate date) {
        Long wagonId = jdbc.sql(RESOLVE)
                .param("wagon_code", wagonCode)
                .query((rs, n) -> rs.getLong(1))
                .optional()
                .orElse(null);
        if (wagonId == null) {
            return List.of();
        }
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return jdbc.sql(DAY)
                .param("wagon_code", wagonCode)
                .param("wagon_id", wagonId)
                .param("day_from", Timestamp.valueOf(from))
                .param("day_to", Timestamp.valueOf(to))
                .param("fetch_limit", 501)
                .query((rs, n) -> {
                    Timestamp getting = rs.getTimestamp("getting_date");
                    LocalDateTime gettingLdt = getting == null ? null : getting.toLocalDateTime();
                    return DbtcStateReportLineFactory.fromGettingAxis(
                            rs.getLong("report_id"),
                            rs.getString("wagon_code"),
                            gettingLdt,
                            rs.getString("station_code"),
                            rs.getString("operation_code"));
                })
                .list();
    }
}
