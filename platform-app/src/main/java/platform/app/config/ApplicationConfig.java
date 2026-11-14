package platform.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import platform.dislocation.DayStateReportQuery;
import platform.dislocation.StateReportDayPort;
import platform.fleet.WagonCardPort;
import platform.fleet.WagonCardQuery;

@Configuration
public class ApplicationConfig {

    @Bean
    WagonCardQuery wagonCardQuery(WagonCardPort port) {
        return new WagonCardQuery(port);
    }

    @Bean
    DayStateReportQuery dayStateReportQuery(StateReportDayPort port) {
        return new DayStateReportQuery(port);
    }
}
