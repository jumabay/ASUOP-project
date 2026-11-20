package platform.oracle.raildb;

import java.time.LocalDate;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import platform.dislocation.StateReportDayPort;
import platform.dislocation.StateReportLine;

/**
 * P4-0 fail-fast stub for future JDBC against RAILDB. Active only under {@code raildb-test}.
 * Does not open a DataSource; all calls fail fast until a later approved stage.
 */
@Component
@Profile("raildb-test")
public class RailDbStateReportDayPortAdapterStub implements StateReportDayPort {

    @Override
    public List<StateReportLine> findLinesForDay(String wagonCode, LocalDate date) {
        throw RailDbJdbcDenied.exception();
    }
}
