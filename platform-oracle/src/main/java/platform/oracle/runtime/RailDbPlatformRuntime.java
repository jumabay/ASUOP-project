package platform.oracle.runtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("raildb-test")
public class RailDbPlatformRuntime {

    @Bean
    PlatformRuntimeView platformRuntimeView() {
        return new PlatformRuntimeView(
                PlatformRuntimeView.KIND_RAILDB_TEST,
                "RAILDB test stubs",
                false,
                false);
    }
}
