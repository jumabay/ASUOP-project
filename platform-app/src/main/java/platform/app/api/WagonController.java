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
import platform.dislocation.DayStateReportQuery;
import platform.dislocation.InvalidDateException;
import platform.fleet.WagonCardQuery;

@RestController
@RequestMapping("/api/v1/wagons")
public class WagonController {

    private final WagonCardQuery wagonCardQuery;
    private final DayStateReportQuery dayStateReportQuery;

    public WagonController(WagonCardQuery wagonCardQuery, DayStateReportQuery dayStateReportQuery) {
        this.wagonCardQuery = wagonCardQuery;
        this.dayStateReportQuery = dayStateReportQuery;
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
