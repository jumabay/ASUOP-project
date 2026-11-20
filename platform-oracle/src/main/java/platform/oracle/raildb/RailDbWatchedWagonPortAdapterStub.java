package platform.oracle.raildb;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WatchedWagonPage;
import platform.fleet.WatchedWagonPort;

/**
 * P4-0 / P5-D RAIL watched stub under {@code raildb-test}.
 * Does not open a DataSource. Returns an empty page with
 * {@code EMPTY_PENDING_G31} — confirmed membership source is missing (G31 OPEN).
 * Does not invent Active / GetActiveWagonList rules.
 */
@Component
@Profile("raildb-test")
public class RailDbWatchedWagonPortAdapterStub implements WatchedWagonPort {

    @Override
    public WatchedWagonPage findPage(WagonRegistryCriteria criteria) {
        return WatchedWagonPage.emptyPendingG31(criteria.page(), criteria.size());
    }
}
