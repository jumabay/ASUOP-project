package platform.oracle.raildb;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WatchedWagonPage;
import platform.fleet.WatchedWagonPort;

/**
 * P4-0 fail-fast stub for future JDBC against RAILDB. Active only under {@code raildb-test}.
 * Does not open a DataSource; all calls fail fast until a later approved stage.
 */
@Component
@Profile("raildb-test")
public class RailDbWatchedWagonPortAdapterStub implements WatchedWagonPort {

    @Override
    public WatchedWagonPage findPage(WagonRegistryCriteria criteria) {
        throw RailDbJdbcDenied.exception();
    }
}
