package platform.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WatchedWagonQueryTest {

    private final WatchedWagonQuery query = new WatchedWagonQuery(criteria -> {
        assertEquals(0, criteria.page());
        assertEquals(50, criteria.size());
        assertEquals(WagonRegistrySort.CODE_ASC, criteria.sort());
        return WatchedWagonPage.of(
                List.of(sample("SYN-00001")),
                criteria.page(),
                criteria.size(),
                1,
                1,
                WatchedMembershipSource.FIXTURE);
    });

    @Test
    void defaultsPageSizeAndSort() {
        WatchedWagonPage page = query.search(null, null, null, null, null, null);
        assertEquals(1, page.content().size());
        assertEquals("SYN-00001", page.content().getFirst().wagonCode());
        assertEquals(WatchedMembershipSource.FIXTURE, page.membershipSource());
        assertTrue(page.membershipNote().contains("FIXTURE"));
    }

    @Test
    void blankFiltersBecomeNull() {
        WatchedWagonQuery q = new WatchedWagonQuery(criteria -> {
            assertNull(criteria.search());
            assertNull(criteria.owner());
            assertNull(criteria.kind());
            return WatchedWagonPage.of(List.of(), 0, 50, 0, 0, WatchedMembershipSource.FIXTURE);
        });
        WatchedWagonPage page = q.search("   ", " ", "\t", 0, 50, "code,asc");
        assertTrue(page.content().isEmpty());
        assertEquals(WatchedMembershipSource.FIXTURE, page.membershipSource());
    }

    @Test
    void passesOwnerAndKind() {
        WatchedWagonQuery q = new WatchedWagonQuery(criteria -> {
            assertEquals("SYN-OWNER", criteria.owner());
            assertEquals("КРЫТЫЙ", criteria.kind());
            return WatchedWagonPage.of(List.of(), 0, 50, 0, 0, WatchedMembershipSource.FIXTURE);
        });
        q.search(null, "SYN-OWNER", "КРЫТЫЙ", 0, 50, "code,asc");
    }

    @Test
    void invalidPageRejected() {
        assertThrows(InvalidPageException.class, () ->
                query.search(null, null, null, -1, 50, null));
    }

    @Test
    void invalidSizeRejected() {
        assertThrows(InvalidPageSizeException.class, () ->
                query.search(null, null, null, 0, 101, null));
    }

    @Test
    void maxWatchedPageSizeAccepted() {
        WatchedWagonQuery q = new WatchedWagonQuery(criteria -> {
            assertEquals(100, criteria.size());
            return WatchedWagonPage.of(List.of(), 0, 100, 0, 0, WatchedMembershipSource.FIXTURE);
        });
        q.search(null, null, null, 0, 100, null);
    }

    @Test
    void invalidSortRejected() {
        assertThrows(InvalidSortException.class, () ->
                query.search(null, null, null, 0, 50, "model,asc"));
    }

    @Test
    void dbtcInventoryParkMembershipNote() {
        WatchedWagonPage page = WatchedWagonPage.of(
                List.of(), 0, 50, 0, 0, WatchedMembershipSource.DBTC_ACTIVE_INVENTORY_PARK);
        assertEquals(WatchedMembershipSource.DBTC_ACTIVE_INVENTORY_PARK, page.membershipSource());
        assertTrue(page.membershipNote().toLowerCase().contains("инвентарный парк"));
        assertTrue(page.membershipNote().contains("На слежении"));
    }

    @Test
    void emptyPendingG31Factory() {
        WatchedWagonPage page = WatchedWagonPage.emptyPendingG31(0, 50);
        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        assertEquals(WatchedMembershipSource.EMPTY_PENDING_G31, page.membershipSource());
        assertTrue(page.membershipNote().contains("G31"));
    }

    private static WatchedWagonItem sample(String code) {
        return new WatchedWagonItem(code, "12-132", "КРЫТЫЙ", "SYN-OWNER", "6900", "SYN-STATION", true);
    }
}
