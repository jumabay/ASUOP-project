package platform.dislocation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DayIntervalTest {

    @Test
    void includesStartAndFractionalSecondBeforeNextMidnight() {
        DayInterval day = DayInterval.of(LocalDate.of(2024, 1, 15));
        assertTrue(day.contains(LocalDateTime.of(2024, 1, 15, 0, 0, 0)));
        assertTrue(day.contains(LocalDateTime.of(2024, 1, 15, 23, 59, 59, 500_000_000)));
    }

    @Test
    void excludesNextMidnight() {
        DayInterval day = DayInterval.of(LocalDate.of(2024, 1, 15));
        assertFalse(day.contains(LocalDateTime.of(2024, 1, 16, 0, 0, 0)));
    }
}
