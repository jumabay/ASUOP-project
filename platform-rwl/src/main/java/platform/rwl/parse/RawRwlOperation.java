package platform.rwl.parse;

import java.time.Instant;
import java.time.LocalDateTime;

/** Intermediate parse DTO before normalization into intake events. */
public final class RawRwlOperation {

    public String wagonNo = "";
    public String stationCode = "";
    public String stationName = "";
    public LocalDateTime operationTimeUtc;
    public String operationCode = "";
    public String operationAsoupCode = "";
    public String operationName = "";
    public String trainIndex = "";
    public String trainNumber = "";
    public String fromStationCode = "";
    public String toStationCode = "";
    public String loadedFlag = "";
    public String cargoName = "";
    public String etsngCode = "";
    public String weightTons = "";
    public Instant fetchedAtUtc;
}
