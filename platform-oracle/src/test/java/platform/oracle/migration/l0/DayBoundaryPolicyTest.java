package platform.oracle.migration.l0;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DayBoundaryPolicyTest {

    @Test
    void halfOpenIncludesStartExcludesNextMidnight() {
        LocalDate day = LocalDate.of(2025, 11, 16);
        assertTrue(DayBoundaryPolicy.contains(day, LocalDateTime.of(2025, 11, 16, 0, 0)));
        assertTrue(DayBoundaryPolicy.contains(day, LocalDateTime.of(2025, 11, 16, 23, 59, 59)));
        assertFalse(DayBoundaryPolicy.contains(day, LocalDateTime.of(2025, 11, 17, 0, 0)));
        assertFalse(DayBoundaryPolicy.contains(day, LocalDateTime.of(2025, 11, 15, 23, 59, 59)));
    }

    @Test
    void documentsOpenTimezoneGap() {
        assertTrue(DayBoundaryPolicy.DOCUMENTATION.contains("R1-Q-TZ OPEN"));
        assertTrue(OpenGaps.stillOpen().contains(OpenGaps.R1_Q_TZ));
    }
}
