package platform.oracle.raildb;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import platform.fleet.WagonCard;
import platform.fleet.WagonCardPort;

/**
 * P4-0 fail-fast stub for future JDBC against RAILDB. Active only under {@code raildb-test}.
 * Does not open a DataSource; all calls fail fast until a later approved stage.
 */
@Component
@Profile("raildb-test")
public class RailDbWagonPortAdapterStub implements WagonCardPort {

    @Override
    public Optional<WagonCard> findByCode(String wagonCode) {
        throw RailDbJdbcDenied.exception();
    }
}
