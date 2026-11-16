package platform.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WatchedWagonQueryTest {

    private final WatchedWagonQuery query = new WatchedWagonQuery(criteria -> {
        assertEquals(0, criteria.page());
        assertEquals(50, criteria.size());
        assertEquals(WagonRegistrySort.CODE_ASC, criteria.sort());
        return new WatchedWagonPage(
                List.of(sample("SYN-00001")),
                criteria.page(),
                criteria.size(),
                1,
                1);
    });

    @Test
    void defaultsPageSizeAndSort() {
        WatchedWagonPage page = query.search(null, null, null, null);
        assertEquals(1, page.content().size());
        assertEquals("SYN-00001", page.content().getFirst().wagonCode());
    }

    @Test
    void blankSearchBecomesNull() {
        WatchedWagonQuery q = new WatchedWagonQuery(criteria -> {
            assertEquals(null, criteria.search());
            return new WatchedWagonPage(List.of(), 0, 50, 0, 0);
        });
        WatchedWagonPage page = q.search("   ", 0, 50, "code,asc");
        assertTrue(page.content().isEmpty());
    }

    @Test
    void invalidPageRejected() {
        assertThrows(InvalidPageException.class, () -> query.search(null, -1, 50, null));
    }

    @Test
    void invalidSizeRejected() {
        assertThrows(InvalidPageSizeException.class, () -> query.search(null, 0, 201, null));
    }

    @Test
    void invalidSortRejected() {
        assertThrows(InvalidSortException.class, () -> query.search(null, 0, 50, "model,asc"));
    }

    private static WatchedWagonItem sample(String code) {
        return new WatchedWagonItem(code, "12-132", "КРЫТЫЙ", "SYN-OWNER", "6900", "SYN-STATION", true);
    }
}
