package platform.oracle.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import platform.dislocation.StateReportLine;
import platform.fleet.WagonCard;

class SyntheticAdaptersTest {

    private SyntheticWagonCardAdapter cardAdapter;
    private SyntheticStateReportDayAdapter dayAdapter;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        cardAdapter = new SyntheticWagonCardAdapter(mapper);
        dayAdapter = new SyntheticStateReportDayAdapter(mapper);
    }

    @Test
    void loadsWagonCard() {
        WagonCard card = cardAdapter.findByCode("SYN-00001").orElseThrow();
        assertEquals("SYN-00001", card.wagonCode());
        assertEquals("12-132", card.model());
        assertTrue(card.inventoryPark());
    }

    @Test
    void unknownWagonEmpty() {
        assertTrue(cardAdapter.findByCode("SYN-UNKNOWN").isEmpty());
    }

    @Test
    void stubCardForCatalogCodeWithoutFile() {
        WagonCard card = cardAdapter.findByCode("SYN-00002").orElseThrow();
        assertEquals("SYN-00002", card.wagonCode());
        assertEquals("12-132", card.model());
    }

    @Test
    void loadsDayHistory() {
        List<StateReportLine> lines = dayAdapter.findLinesForDay("SYN-00001", LocalDate.of(2024, 1, 15));
        assertEquals(3, lines.size());
    }

    @Test
    void emptyDay() {
        List<StateReportLine> lines = dayAdapter.findLinesForDay("SYN-00001", LocalDate.of(2024, 1, 16));
        assertTrue(lines.isEmpty());
    }

    @Test
    void dayWithoutFixtureIsEmpty() {
        List<StateReportLine> lines = dayAdapter.findLinesForDay("SYN-00001", LocalDate.of(2024, 1, 17));
        assertTrue(lines.isEmpty());
    }
}
