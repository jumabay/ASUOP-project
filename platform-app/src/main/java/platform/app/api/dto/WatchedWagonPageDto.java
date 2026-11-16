package platform.app.api.dto;

import java.util.List;

public record WatchedWagonPageDto(
        List<WatchedWagonItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
