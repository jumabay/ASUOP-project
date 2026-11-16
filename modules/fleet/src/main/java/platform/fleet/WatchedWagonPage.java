package platform.fleet;

import java.util.List;
import java.util.Objects;

public record WatchedWagonPage(
        List<WatchedWagonItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public WatchedWagonPage {
        Objects.requireNonNull(content, "content");
        content = List.copyOf(content);
    }

    public static int computeTotalPages(long totalElements, int size) {
        return WagonRegistryPage.computeTotalPages(totalElements, size);
    }
}
