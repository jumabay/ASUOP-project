package platform.app.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import platform.app.api.dto.PlatformRuntimeDto;
import platform.oracle.runtime.PlatformRuntimeView;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformRuntimeController {

    private final PlatformRuntimeView runtime;

    public PlatformRuntimeController(PlatformRuntimeView runtime) {
        this.runtime = runtime;
    }

    @GetMapping("/runtime")
    public PlatformRuntimeDto runtime() {
        return new PlatformRuntimeDto(
                runtime.dataSourceKind(),
                runtime.displayLabel(),
                runtime.registryConnected(),
                runtime.watchedConnected());
    }
}
