package platform.fleet;

import java.util.Objects;

public final class WagonRegistryQuery {

    private final WagonRegistryPort port;

    public WagonRegistryQuery(WagonRegistryPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public WagonRegistryPage search(
            String search,
            String owner,
            String kind,
            Integer page,
            Integer size,
            String sort) {
        int resolvedPage = page == null ? WagonRegistryCriteria.DEFAULT_PAGE : page;
        int resolvedSize = size == null ? WagonRegistryCriteria.DEFAULT_SIZE : size;
        WagonRegistryCriteria criteria = new WagonRegistryCriteria(
                normalizeOptional(search),
                normalizeOptional(owner),
                normalizeOptional(kind),
                resolvedPage,
                resolvedSize,
                WagonRegistrySort.fromParam(sort));
        return port.findPage(criteria);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
