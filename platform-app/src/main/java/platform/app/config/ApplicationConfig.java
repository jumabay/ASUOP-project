package platform.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import platform.dislocation.DayStateReportQuery;
import platform.dislocation.StateReportDayPort;
import platform.fleet.WagonCardPort;
import platform.fleet.WagonCardQuery;
import platform.fleet.WagonRegistryPort;
import platform.fleet.WagonRegistryQuery;
import platform.fleet.WatchedWagonPort;
import platform.fleet.WatchedWagonQuery;

@Configuration
public class ApplicationConfig {

    @Bean
    WagonCardQuery wagonCardQuery(WagonCardPort port) {
        return new WagonCardQuery(port);
    }

    @Bean
    WagonRegistryQuery wagonRegistryQuery(WagonRegistryPort port) {
        return new WagonRegistryQuery(port);
    }

    @Bean
    WatchedWagonQuery watchedWagonQuery(WatchedWagonPort port) {
        return new WatchedWagonQuery(port);
    }

    @Bean
    DayStateReportQuery dayStateReportQuery(StateReportDayPort port) {
        return new DayStateReportQuery(port);
    }
}
