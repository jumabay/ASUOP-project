package platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistryPort;
import platform.fleet.WagonRegistrySort;
import platform.fleet.WatchedMembershipSource;
import platform.fleet.WatchedWagonPage;
import platform.fleet.WatchedWagonPort;
import platform.oracle.raildb.RailDbJdbcDenied;
import platform.oracle.raildb.RailDbProfileMarker;
import platform.oracle.raildb.RailDbStateReportDayPortAdapterStub;
import platform.oracle.raildb.RailDbWagonPortAdapterStub;
import platform.oracle.raildb.RailDbWagonRegistryPortAdapterStub;
import platform.oracle.raildb.RailDbWatchedWagonPortAdapterStub;
import platform.oracle.synthetic.SyntheticWagonCardAdapter;

@SpringBootTest
@ActiveProfiles("raildb-test")
class RailDbTestProfileFailFastTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    Environment environment;

    @Autowired
    WagonCardPort wagonCardPort;

    @Autowired
    WagonRegistryPort wagonRegistryPort;

    @Autowired
    WatchedWagonPort watchedWagonPort;

    @Autowired
    StateReportDayPort stateReportDayPort;

    @Autowired
    RailDbProfileMarker railDbProfileMarker;

    @Test
    void raildbTestContextStartsWithoutDataSourceOrSyntheticFallback() {
        assertTrue(environment.matchesProfiles("raildb-test"));
        assertFalse(environment.matchesProfiles("synthetic"));

        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(DataSource.class));
        assertFalse(context.containsBean("dataSource"));

        assertFalse(environment.containsProperty("spring.datasource.url"));
        assertFalse(environment.containsProperty("spring.datasource.username"));
        assertFalse(environment.containsProperty("spring.datasource.password"));

        assertNotNull(railDbProfileMarker);
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(SyntheticWagonCardAdapter.class));
    }

    @Test
    void allReadPortsAreRailDbStubsNotSynthetic() {
        assertInstanceOf(RailDbWagonPortAdapterStub.class, wagonCardPort);
        assertInstanceOf(RailDbWagonRegistryPortAdapterStub.class, wagonRegistryPort);
        assertInstanceOf(RailDbWatchedWagonPortAdapterStub.class, watchedWagonPort);
        assertInstanceOf(RailDbStateReportDayPortAdapterStub.class, stateReportDayPort);
    }

    @Test
    void eachPortMethodFailsFastWithDeniedDiagnostic() {
        WagonRegistryCriteria criteria =
                new WagonRegistryCriteria(null, null, null, 0, 10, WagonRegistrySort.CODE_ASC);

        assertDenied(() -> wagonCardPort.findByCode("12345678"));
        assertDenied(() -> wagonRegistryPort.findPage(criteria));
        assertDenied(() -> stateReportDayPort.findLinesForDay("12345678", LocalDate.of(2024, 1, 1)));
    }

    @Test
    void watchedReturnsEmptyPendingG31WithoutJdbc() {
        WagonRegistryCriteria criteria =
                new WagonRegistryCriteria(null, null, null, 0, 10, WagonRegistrySort.CODE_ASC);

        WatchedWagonPage page = watchedWagonPort.findPage(criteria);
        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        assertEquals(WatchedMembershipSource.EMPTY_PENDING_G31, page.membershipSource());
        assertTrue(page.membershipNote().contains("G31"));
    }

    private static void assertDenied(Runnable call) {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, call::run);
        assertTrue(ex.getMessage().contains("DENIED"));
        assertTrue(ex.getMessage().contains("P4-0"));
        assertEquals(RailDbJdbcDenied.MESSAGE, ex.getMessage());
    }
}
