package platform.oracle.jdbc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import platform.fleet.WagonRegistryPage;
import platform.fleet.WagonRegistryPort;

/**
 * Registry JDBC remains out of P5-E approval — empty stub under {@code dbtc-ro}.
 * Watched list uses {@link DbtcJdbcInventoryParkWatchedAdapter} (P5-G31).
 */
@Configuration
@Profile("dbtc-ro")
public class DbtcRoListStubs {

    @Bean
    WagonRegistryPort dbtcRoRegistryStub() {
        // Empty content + FE uses platform runtime registryConnected=false
        // → «Источник не подключён» (not “no wagons match filter”).
        return criteria -> new WagonRegistryPage(
                java.util.List.of(), criteria.page(), criteria.size(), 0, 0);
    }

}
