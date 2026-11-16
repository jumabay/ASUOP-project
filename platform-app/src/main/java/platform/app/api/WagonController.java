package platform.app.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import platform.app.api.dto.StateReportDayDto;
import platform.app.api.dto.WagonCardDto;
import platform.app.api.dto.WagonRegistryPageDto;
import platform.app.api.dto.WatchedWagonPageDto;
import platform.dislocation.DayStateReportQuery;
import platform.dislocation.InvalidDateException;
import platform.fleet.WagonCardQuery;
import platform.fleet.WagonRegistryQuery;
import platform.fleet.WatchedWagonQuery;

@RestController
@RequestMapping("/api/v1/wagons")
public class WagonController {

    private final WagonCardQuery wagonCardQuery;
    private final DayStateReportQuery dayStateReportQuery;
    private final WagonRegistryQuery wagonRegistryQuery;
    private final WatchedWagonQuery watchedWagonQuery;

    public WagonController(
            WagonCardQuery wagonCardQuery,
            DayStateReportQuery dayStateReportQuery,
            WagonRegistryQuery wagonRegistryQuery,
            WatchedWagonQuery watchedWagonQuery) {
        this.wagonCardQuery = wagonCardQuery;
        this.dayStateReportQuery = dayStateReportQuery;
        this.wagonRegistryQuery = wagonRegistryQuery;
        this.watchedWagonQuery = watchedWagonQuery;
    }

    @GetMapping
    public ResponseEntity<WagonRegistryPageDto> getWagonRegistry(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(WagonApiMapper.toDto(wagonRegistryQuery.search(search, page, size, sort)));
    }

    /** Literal path before /{wagonCode} so "watched" is never treated as a wagon code. */
    @GetMapping("/watched")
    public ResponseEntity<WatchedWagonPageDto> getWatchedWagons(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(WagonApiMapper.toDto(watchedWagonQuery.search(search, page, size, sort)));
    }

    @GetMapping("/{wagonCode}")
    public ResponseEntity<WagonCardDto> getWagonCard(@PathVariable String wagonCode) {
        return ResponseEntity.ok(WagonApiMapper.toDto(wagonCardQuery.getByCode(wagonCode)));
    }

    @GetMapping("/{wagonCode}/state-reports")
    public ResponseEntity<StateReportDayDto> getStateReports(
            @PathVariable String wagonCode,
            @RequestParam(value = "date", required = false) String date) {
        if (date == null || date.isBlank()) {
            throw new InvalidDateException("Query parameter 'date' is required (YYYY-MM-DD)");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(date.trim());
        } catch (DateTimeParseException ex) {
            throw new InvalidDateException("Invalid date format: " + date);
        }
        // Unified rule: unknown wagon → 404 for day history as well.
        wagonCardQuery.getByCode(wagonCode);
        return ResponseEntity.ok(WagonApiMapper.toDto(dayStateReportQuery.getForDay(wagonCode, day)));
    }
}
