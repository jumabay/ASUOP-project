package platform.oracle.migration.l0.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KitRecord(
        Long kitId,
        String filename,
        int type,
        LocalDateTime importTs,
        LocalDateTime gettingTs,
        String scenario
) {
}
