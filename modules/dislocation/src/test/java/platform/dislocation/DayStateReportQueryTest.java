package platform.dislocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DayStateReportQueryTest {

    @Test
    void returnsEmptyDay() {
        DayStateReportQuery query = new DayStateReportQuery((code, date) -> List.of());
        StateReportDay day = query.getForDay("SYN-00001", LocalDate.of(2024, 1, 16));
        assertTrue(day.lines().isEmpty());
    }

    @Test
    void throwsWhenOverLimit() {
        List<StateReportLine> many = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            many.add(new StateReportLine(
                    i,
                    "SYN-00001",
                    LocalDateTime.of(2024, 1, 15, 1, 0),
                    "6900",
                    "01"));
        }
        DayStateReportQuery query = new DayStateReportQuery((code, date) -> many);
        ResultLimitExceededException ex = assertThrows(
                ResultLimitExceededException.class,
                () -> query.getForDay("SYN-00001", LocalDate.of(2024, 1, 15)));
        assertEquals(501, ex.actualCount());
        assertEquals(500, ex.limit());
    }
}
