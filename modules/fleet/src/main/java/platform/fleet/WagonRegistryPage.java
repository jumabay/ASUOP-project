package platform.fleet;

import java.util.List;
import java.util.Objects;

public record WagonRegistryPage(
        List<WagonRegistryItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public WagonRegistryPage {
        Objects.requireNonNull(content, "content");
        content = List.copyOf(content);
    }

    public static int computeTotalPages(long totalElements, int size) {
        if (totalElements <= 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }
}
