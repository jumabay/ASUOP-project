package platform.oracle.raildb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistrySort;
import platform.fleet.WatchedMembershipSource;
import platform.fleet.WatchedWagonPage;

class RailDbWatchedWagonPortAdapterStubTest {

    private final RailDbWatchedWagonPortAdapterStub stub = new RailDbWatchedWagonPortAdapterStub();

    @Test
    void returnsEmptyPendingG31WithoutInventingMembership() {
        WatchedWagonPage page = stub.findPage(
                new WagonRegistryCriteria(null, 0, 50, WagonRegistrySort.CODE_ASC));

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        assertEquals(0, page.totalPages());
        assertEquals(WatchedMembershipSource.EMPTY_PENDING_G31, page.membershipSource());
        assertTrue(page.membershipNote().contains("G31"));
        assertEquals(0, page.page());
        assertEquals(50, page.size());
    }
}
