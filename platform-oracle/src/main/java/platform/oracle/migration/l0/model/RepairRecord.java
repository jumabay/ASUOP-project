package platform.oracle.migration.l0.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepairRecord(
        Long repairId,
        Long wagonId,
        String code,
        Long repairTypeId,
        LocalDateTime beginTs,
        LocalDateTime endingTs,
        @JsonProperty("is_open_via_current") boolean isOpenViaCurrent,
        String scenario
) {
}
