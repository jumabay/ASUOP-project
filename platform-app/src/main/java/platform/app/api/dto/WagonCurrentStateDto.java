package platform.app.api.dto;

public record WagonCurrentStateDto(
        String stationCode,
        String operationCode,
        String dislocationTime,
        String trainIndex
) {
}
