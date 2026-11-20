package platform.oracle.migration.l0.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WagonRecord(
        Long wagonId,
        String code,
        LocalDateTime createTs,
        Long wagonTypeId,
        String ownerCode,
        String scenario
) {
}
