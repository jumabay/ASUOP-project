package platform.oracle.runtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dbtc-ro")
public class DbtcRoPlatformRuntime {

    @Bean
    PlatformRuntimeView platformRuntimeView() {
        return new PlatformRuntimeView(
                PlatformRuntimeView.KIND_DBTC_RO,
                "DBTC read-only (time-boxed)",
                false,
                false);
    }
}
