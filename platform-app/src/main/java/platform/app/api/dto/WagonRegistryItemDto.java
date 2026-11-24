package platform.app.api.dto;

public record WagonRegistryItemDto(
        String wagonCode,
        String model,
        String wagonKind,
        String ownerName,
        String homeStationCode,
        String homeStationName,
        Boolean inventoryPark
) {
}
