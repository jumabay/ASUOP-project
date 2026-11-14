package platform.app;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    @Test
    void verifiesModularStructure() {
        ApplicationModules modules = ApplicationModules.of(RailwayPlatformApplication.class);
        modules.verify();
    }
}
