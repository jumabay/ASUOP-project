package platform.oracle.runtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("synthetic")
public class SyntheticPlatformRuntime {

    @Bean
    PlatformRuntimeView platformRuntimeView() {
        return new PlatformRuntimeView(
                PlatformRuntimeView.KIND_SYNTHETIC,
                "Synthetic (offline)",
                true,
                true);
    }
}
