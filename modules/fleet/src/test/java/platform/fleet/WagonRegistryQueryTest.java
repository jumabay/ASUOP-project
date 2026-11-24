package platform.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WagonRegistryQueryTest {

    @Test
    void appliesDefaults() {
        WagonRegistryQuery query = new WagonRegistryQuery(criteria -> {
            assertEquals(0, criteria.page());
            assertEquals(50, criteria.size());
            assertEquals(WagonRegistrySort.CODE_ASC, criteria.sort());
            return new WagonRegistryPage(List.of(), 0, 50, 0, 0);
        });
        WagonRegistryPage page = query.search(null, null, null, null);
        assertEquals(0, page.totalElements());
        assertEquals(0, page.totalPages());
    }

    @Test
    void trimsBlankSearchToNull() {
        WagonRegistryQuery query = new WagonRegistryQuery(criteria -> {
            assertEquals(null, criteria.search());
            return new WagonRegistryPage(List.of(), 0, 50, 0, 0);
        });
        query.search("   ", 0, 50, "code,asc");
    }

    @Test
    void rejectsNegativePage() {
        WagonRegistryQuery query = new WagonRegistryQuery(c -> null);
        assertThrows(InvalidPageException.class, () -> query.search(null, -1, 50, "code,asc"));
    }

    @Test
    void rejectsInvalidSize() {
        WagonRegistryQuery query = new WagonRegistryQuery(c -> null);
        assertThrows(InvalidPageSizeException.class, () -> query.search(null, 0, 201, "code,asc"));
        assertThrows(InvalidPageSizeException.class, () -> query.search(null, 0, 0, "code,asc"));
    }

    @Test
    void rejectsInvalidSort() {
        WagonRegistryQuery query = new WagonRegistryQuery(c -> null);
        assertThrows(InvalidSortException.class, () -> query.search(null, 0, 50, "model,asc"));
    }

    @Test
    void acceptsDescSort() {
        WagonRegistryQuery query = new WagonRegistryQuery(criteria -> {
            assertEquals(WagonRegistrySort.CODE_DESC, criteria.sort());
            return new WagonRegistryPage(List.of(sampleItem("SYN-00002")), 0, 50, 1, 1);
        });
        assertEquals(1, query.search(null, 0, 50, "code,desc").content().size());
        assertTrue(query.search(null, 0, 50, "code,desc").content().getFirst().wagonCode().startsWith("SYN-"));
    }

    private static WagonRegistryItem sampleItem(String code) {
        return new WagonRegistryItem(code, "12-132", "КРЫТЫЙ", "SYN-OWNER", "6900", "SYN-STATION", true);
    }
}
