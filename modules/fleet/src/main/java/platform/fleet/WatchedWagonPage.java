package platform.fleet;

import java.util.List;
import java.util.Objects;

/**
 * Paginated watched list plus additive membership honesty fields (P5-D).
 */
public record WatchedWagonPage(
        List<WatchedWagonItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        WatchedMembershipSource membershipSource,
        String membershipNote
) {
    public WatchedWagonPage {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(membershipSource, "membershipSource");
        content = List.copyOf(content);
        if (membershipNote == null || membershipNote.isBlank()) {
            membershipNote = membershipSource.defaultNote();
        }
    }

    public static WatchedWagonPage of(
            List<WatchedWagonItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            WatchedMembershipSource membershipSource) {
        return new WatchedWagonPage(
                content,
                page,
                size,
                totalElements,
                totalPages,
                membershipSource,
                membershipSource.defaultNote());
    }

    public static WatchedWagonPage emptyPendingG31(int page, int size) {
        return of(
                List.of(),
                page,
                size,
                0,
                0,
                WatchedMembershipSource.EMPTY_PENDING_G31);
    }

    public static WatchedWagonPage emptySourceNotConnected(int page, int size) {
        return of(
                List.of(),
                page,
                size,
                0,
                0,
                WatchedMembershipSource.SOURCE_NOT_CONNECTED);
    }

    public static int computeTotalPages(long totalElements, int size) {
        return WagonRegistryPage.computeTotalPages(totalElements, size);
    }
}
