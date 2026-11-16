package platform.app.api;

import java.util.List;
import platform.app.api.dto.StateReportDayDto;
import platform.app.api.dto.StateReportLineDto;
import platform.app.api.dto.WagonCardDto;
import platform.app.api.dto.WagonRegistryItemDto;
import platform.app.api.dto.WagonRegistryPageDto;
import platform.dislocation.StateReportDay;
import platform.dislocation.StateReportLine;
import platform.fleet.WagonCard;
import platform.fleet.WagonRegistryPage;

final class WagonApiMapper {

    private WagonApiMapper() {
    }

    static WagonCardDto toDto(WagonCard card) {
        return new WagonCardDto(
                card.wagonCode(),
                card.model(),
                card.wagonKind(),
                card.ownerName(),
                card.homeStationCode(),
                card.homeStationName(),
                card.inventoryPark());
    }

    static StateReportDayDto toDto(StateReportDay day) {
        List<StateReportLineDto> lines = day.lines().stream().map(WagonApiMapper::toDto).toList();
        return new StateReportDayDto(day.wagonCode(), day.date(), lines);
    }

    private static StateReportLineDto toDto(StateReportLine line) {
        return new StateReportLineDto(
                line.reportId(),
                line.wagonCode(),
                line.dislocationTime(),
                line.stationCode(),
                line.operationCode());
    }

    static WagonRegistryPageDto toDto(WagonRegistryPage page) {
        List<WagonRegistryItemDto> content = page.content().stream()
                .map(item -> new WagonRegistryItemDto(item.wagonCode()))
                .toList();
        return new WagonRegistryPageDto(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
