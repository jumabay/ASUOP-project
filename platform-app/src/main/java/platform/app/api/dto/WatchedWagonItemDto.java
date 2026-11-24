package platform.app.api.dto;

public record WatchedWagonItemDto(
        String wagonCode,
        String model,
        String wagonKind,
        String ownerName,
        String homeStationCode,
        String homeStationName,
        Boolean inventoryPark
) {
}
