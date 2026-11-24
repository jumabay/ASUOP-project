package platform.fleet;

import java.util.Objects;

public final class WatchedWagonQuery {

    public static final int MAX_SIZE = 100;

    private final WatchedWagonPort port;

    public WatchedWagonQuery(WatchedWagonPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public WatchedWagonPage search(
            String search,
            String owner,
            String kind,
            Integer page,
            Integer size,
            String sort) {
        int resolvedPage = page == null ? WagonRegistryCriteria.DEFAULT_PAGE : page;
        int resolvedSize = size == null ? WagonRegistryCriteria.DEFAULT_SIZE : size;
        if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
            throw new InvalidPageSizeException("size must be between 1 and " + MAX_SIZE);
        }
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
