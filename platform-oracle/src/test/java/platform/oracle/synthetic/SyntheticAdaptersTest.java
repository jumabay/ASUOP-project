package platform.oracle.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import platform.dislocation.StateReportLine;
import platform.fleet.WagonCard;
import platform.fleet.WagonCurrentState;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistryItem;
import platform.fleet.WagonRegistryPage;
import platform.fleet.WagonRegistrySort;

class SyntheticAdaptersTest {

    private SyntheticWagonCardAdapter cardAdapter;
    private SyntheticStateReportDayAdapter dayAdapter;
    private SyntheticWagonRegistryAdapter registryAdapter;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        cardAdapter = new SyntheticWagonCardAdapter(mapper);
        dayAdapter = new SyntheticStateReportDayAdapter(mapper);
        registryAdapter = new SyntheticWagonRegistryAdapter(mapper);
    }

    @Test
    void loadsWagonCard() {
        WagonCard card = cardAdapter.findByCode("SYN-00001").orElseThrow();
        assertEquals("SYN-00001", card.wagonCode());
        assertEquals("12-132", card.model());
        assertTrue(card.inventoryPark());
        assertNotNull(card.currentState());
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
        assertNull(card.currentState());
    }

    @Test
    void currentStateDiffersFromLastDayLineOnSyn00001() {
        WagonCard card = cardAdapter.findByCode("SYN-00001").orElseThrow();
        WagonCurrentState current = card.currentState();
        assertNotNull(current);

        List<StateReportLine> lines =
                dayAdapter.findLinesForDay("SYN-00001", LocalDate.of(2024, 1, 15));
        assertEquals(3, lines.size());
        StateReportLine last = lines.getLast();
        assertEquals("6902", last.stationCode());
        assertEquals("01", last.operationCode());
        assertEquals("2024-01-15T23:59:59.500", last.snapshotTime().toString());
        assertEquals(last.snapshotTime(), last.dislocationTime());

        assertEquals("7100", current.stationCode());
        assertEquals("05", current.operationCode());
        assertEquals("2024-01-16T06:00:00", current.dislocationTime());
        assertEquals("TRN-DIFF", current.trainIndex());
        assertNotEquals(last.stationCode(), current.stationCode());
        assertNotEquals(last.operationCode(), current.operationCode());
        assertNotEquals(last.dislocationTime().toString(), current.dislocationTime());
    }

    @Test
    void registryItemsIncludePassportFields() {
        WagonRegistryPage page = registryAdapter.findPage(
                new WagonRegistryCriteria("SYN-00001", null, null, 0, 10, WagonRegistrySort.CODE_ASC));
        WagonRegistryItem item = page.content().stream()
                .filter(i -> "SYN-00001".equals(i.wagonCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("12-132", item.model());
        assertEquals("КРЫТЫЙ", item.wagonKind());
        assertEquals("SYN-OWNER", item.ownerName());
        assertEquals("6900", item.homeStationCode());
        assertEquals("SYN-STATION", item.homeStationName());
        assertTrue(item.inventoryPark());
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
