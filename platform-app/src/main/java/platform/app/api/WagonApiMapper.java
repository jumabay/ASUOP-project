package platform.app.api;

import java.time.LocalDateTime;
import java.util.List;
import platform.app.api.dto.StateReportDayDto;
import platform.app.api.dto.StateReportLineDto;
import platform.app.api.dto.WagonCardDto;
import platform.app.api.dto.WagonCurrentStateDto;
import platform.app.api.dto.WagonRegistryItemDto;
import platform.app.api.dto.WagonRegistryPageDto;
import platform.app.api.dto.WatchedWagonItemDto;
import platform.app.api.dto.WatchedWagonPageDto;
import platform.dislocation.StateReportDay;
import platform.dislocation.StateReportLine;
import platform.fleet.WagonCard;
import platform.fleet.WagonCurrentState;
import platform.fleet.WagonRegistryPage;
import platform.fleet.WatchedWagonPage;

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
                card.inventoryPark(),
                toDto(card.currentState()));
    }

    private static WagonCurrentStateDto toDto(WagonCurrentState current) {
        if (current == null) {
            return null;
        }
        return new WagonCurrentStateDto(
                current.stationCode(),
                current.operationCode(),
                current.dislocationTime(),
                current.trainIndex());
    }

    static StateReportDayDto toDto(StateReportDay day) {
        List<StateReportLineDto> lines = day.lines().stream().map(WagonApiMapper::toDto).toList();
        return new StateReportDayDto(day.wagonCode(), day.date(), lines);
    }

    private static StateReportLineDto toDto(StateReportLine line) {
        LocalDateTime snapshot = line.snapshotTime();
        return new StateReportLineDto(
                line.reportId(),
                line.wagonCode(),
                snapshot,
                snapshot, // deprecated alias — always equal to snapshotTime (T1)
                line.stationCode(),
                line.operationCode());
    }

    static WagonRegistryPageDto toDto(WagonRegistryPage page) {
        List<WagonRegistryItemDto> content = page.content().stream()
                .map(item -> new WagonRegistryItemDto(
                        item.wagonCode(),
                        item.model(),
                        item.wagonKind(),
                        item.ownerName(),
                        item.homeStationCode(),
                        item.homeStationName(),
                        item.inventoryPark()))
                .toList();
        return new WagonRegistryPageDto(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    static WatchedWagonPageDto toDto(WatchedWagonPage page) {
        List<WatchedWagonItemDto> content = page.content().stream()
                .map(item -> new WatchedWagonItemDto(
                        item.wagonCode(),
                        item.model(),
                        item.wagonKind(),
                        item.ownerName(),
                        item.homeStationCode(),
                        item.homeStationName(),
                        item.inventoryPark()))
                .toList();
        return new WatchedWagonPageDto(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.membershipSource().name(),
                page.membershipNote());
    }
}
