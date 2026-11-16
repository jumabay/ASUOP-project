package platform.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import platform.dislocation.StateReportDayPort;
import platform.fleet.WagonCardPort;
import platform.fleet.WagonRegistryPort;
import platform.fleet.WatchedWagonPort;
import platform.oracle.raildb.RailDbProfileMarker;
import platform.oracle.synthetic.SyntheticWagonCardAdapter;

@SpringBootTest
@ActiveProfiles("synthetic")
class NoOracleDataSourceTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    Environment environment;

    @Test
    void syntheticProfileHasNoDataSourceBean() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(DataSource.class));
        assertFalse(context.containsBean("dataSource"));
    }

    @Test
    void syntheticProfileWiresPortAdaptersWithoutRailDbMarker() {
        assertTrue(environment.matchesProfiles("synthetic"));
        assertFalse(environment.matchesProfiles("raildb-test"));

        assertNotNull(context.getBean(WagonCardPort.class));
        assertNotNull(context.getBean(WagonRegistryPort.class));
        assertNotNull(context.getBean(WatchedWagonPort.class));
        assertNotNull(context.getBean(StateReportDayPort.class));
        assertNotNull(context.getBean(SyntheticWagonCardAdapter.class));

        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(RailDbProfileMarker.class));
    }

    @Test
    void syntheticHasNoDatasourcePropertiesRegardlessOfRailDbPasswordEnv() {
        // Context already started under synthetic without requiring RAIL_DB_PASSWORD.
        assertFalse(environment.containsProperty("spring.datasource.url"));
        assertFalse(environment.containsProperty("spring.datasource.username"));
        assertFalse(environment.containsProperty("spring.datasource.password"));
        assertNotNull(context.getBean(WagonCardPort.class));
    }
}
