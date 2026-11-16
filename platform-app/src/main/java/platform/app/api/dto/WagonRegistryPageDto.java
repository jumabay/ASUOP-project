package platform.app.api.dto;

import java.util.List;

public record WagonRegistryPageDto(
        List<WagonRegistryItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
