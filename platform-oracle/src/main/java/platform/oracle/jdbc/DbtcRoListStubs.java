package platform.oracle.jdbc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import platform.fleet.WagonRegistryPage;
import platform.fleet.WagonRegistryPort;
import platform.fleet.WatchedWagonPage;
import platform.fleet.WatchedWagonPort;

/**
 * Registry/watched JDBC remain out of P5-E approval — empty stubs under {@code dbtc-ro}.
 */
@Configuration
@Profile("dbtc-ro")
public class DbtcRoListStubs {

    @Bean
    WagonRegistryPort dbtcRoRegistryStub() {
        return criteria -> new WagonRegistryPage(
                java.util.List.of(), criteria.page(), criteria.size(), 0, 0);
    }

    @Bean
    WatchedWagonPort dbtcRoWatchedStub() {
        return criteria -> WatchedWagonPage.emptyPendingG31(criteria.page(), criteria.size());
    }
}
