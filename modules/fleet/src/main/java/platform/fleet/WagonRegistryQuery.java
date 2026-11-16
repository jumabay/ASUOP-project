package platform.fleet;

import java.util.Objects;

public final class WagonRegistryQuery {

    private final WagonRegistryPort port;

    public WagonRegistryQuery(WagonRegistryPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public WagonRegistryPage search(
            String search,
            Integer page,
            Integer size,
            String sort) {
        int resolvedPage = page == null ? WagonRegistryCriteria.DEFAULT_PAGE : page;
        int resolvedSize = size == null ? WagonRegistryCriteria.DEFAULT_SIZE : size;
        String normalizedSearch = search == null ? null : search.trim();
        if (normalizedSearch != null && normalizedSearch.isEmpty()) {
            normalizedSearch = null;
        }
        WagonRegistryCriteria criteria = new WagonRegistryCriteria(
                normalizedSearch,
                resolvedPage,
                resolvedSize,
                WagonRegistrySort.fromParam(sort));
        return port.findPage(criteria);
    }
}
