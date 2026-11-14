package platform.fleet;

import java.util.Objects;

public final class WagonCardQuery {

    private final WagonCardPort port;

    public WagonCardQuery(WagonCardPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public WagonCard getByCode(String wagonCode) {
        String normalized = normalizeCode(wagonCode);
        return port.findByCode(normalized)
                .orElseThrow(() -> new WagonNotFoundException(normalized));
    }

    static String normalizeCode(String wagonCode) {
        if (wagonCode == null || wagonCode.isBlank()) {
            throw new InvalidWagonCodeException("Wagon code must not be blank");
        }
        return wagonCode.trim();
    }
}
