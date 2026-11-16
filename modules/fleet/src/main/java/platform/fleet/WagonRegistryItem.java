package platform.fleet;

import java.util.Objects;

public record WagonRegistryItem(String wagonCode) {
    public WagonRegistryItem {
        Objects.requireNonNull(wagonCode, "wagonCode");
    }
}
