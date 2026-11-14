package platform.dislocation;

import java.time.LocalDate;
import java.util.List;

/**
 * Data-access port for day dislocation history (owned by dislocation).
 */
public interface StateReportDayPort {

    /**
     * Returns lines for the wagon that fall in {@code [date 00:00, date+1 00:00)}.
     * Does not silently truncate; caller enforces the 500 limit.
     */
    List<StateReportLine> findLinesForDay(String wagonCode, LocalDate date);
}
