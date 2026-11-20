package platform.oracle.migration.l0.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentStateRecord(
        Long wagonId,
        String code,
        Long stationId,
        Long operationId,
        LocalDateTime locationTs,
        Long currentRepairId
) {
}
