package platform.app.api.dto;

import java.util.List;

/**
 * Watched page DTO. {@code membershipSource} is additive (P5-D, non-breaking).
 */
public record WatchedWagonPageDto(
        List<WatchedWagonItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String membershipSource,
        String membershipNote
) {
}
