package platform.oracle.migration.l0.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TapeEventRecord(
        Long eventId,
        Long wagonId,
        String code,
        LocalDateTime gettingTs,
        LocalDateTime locationTs,
        Long stationId,
        Long operationId,
        Long kitId,
        String provenanceKey,
        String contentHash,
        String scenario
) {
}
