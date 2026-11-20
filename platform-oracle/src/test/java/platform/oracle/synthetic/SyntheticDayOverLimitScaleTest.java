package platform.oracle.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import platform.dislocation.DayStateReportQuery;
import platform.dislocation.ResultLimitExceededException;
import platform.dislocation.StateReportLine;

/**
 * T3: 501+ day lines → RESULT_LIMIT_EXCEEDED (query layer).
 *
 * <p>Run: {@code .\scripts\gradle-jdk21.ps1 :platform-oracle:testScale}
 */
@Tag("scale")
class SyntheticDayOverLimitScaleTest {

    @Test
    void t3_generated501LinesExceedsDayLimit() {
        LocalDate date = LocalDate.of(2024, 1, 18);
        List<StateReportLine> lines =
                SyntheticDayLineGenerator.generate("SYN-00001", date, DayStateReportQuery.MAX_LINES + 1);
        assertEquals(501, lines.size());

        var dayAdapter = new SyntheticStateReportDayAdapter(
                new ObjectMapper(),
                Map.of(SyntheticStateReportDayAdapter.overlayKey("SYN-00001", date), lines));
        var query = new DayStateReportQuery(dayAdapter);

        ResultLimitExceededException ex = assertThrows(
                ResultLimitExceededException.class,
                () -> query.getForDay("SYN-00001", date));
        assertEquals("SYN-00001", ex.wagonCode());
        assertEquals(date, ex.date());
        assertEquals(501, ex.actualCount());
        assertEquals(DayStateReportQuery.MAX_LINES, ex.limit());
        assertTrue(ex.getMessage().contains("limit"));
    }

    @Test
    void t3_exactly500LinesIsAccepted() {
        LocalDate date = LocalDate.of(2024, 1, 18);
        List<StateReportLine> lines =
                SyntheticDayLineGenerator.generate("SYN-00001", date, DayStateReportQuery.MAX_LINES);
        var dayAdapter = new SyntheticStateReportDayAdapter(
                new ObjectMapper(),
                Map.of(SyntheticStateReportDayAdapter.overlayKey("SYN-00001", date), lines));
        var day = new DayStateReportQuery(dayAdapter).getForDay("SYN-00001", date);
        assertEquals(500, day.lines().size());
    }
}
