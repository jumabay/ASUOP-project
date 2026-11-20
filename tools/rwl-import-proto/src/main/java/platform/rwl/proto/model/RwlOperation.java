package platform.rwl.proto.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Normalized dislocation operation from RWL {@code position} + {@code vagon_info}.
 *
 * <p>Event identity hypothesis (not a documented RWL event id):
 * {@code wagon_no|utc_datetime|station_code|operation_asoup_code|train_index}.
 * {@code operation_id} from API is treated as operation type, not event id.
 */
public final class RwlOperation {

    private final String wagonNo;
    private final String stationCode;
    private final String stationName;
    private final LocalDateTime operationTimeUtc;
    private final String operationCode;
    private final String operationAsoupCode;
    private final String operationName;
    private final String operationTypeId;
    private final String trainIndex;
    private final String trainNumber;
    private final String fromStationCode;
    private final String fromStationName;
    private final String toStationCode;
    private final String toStationName;
    private final String loadedFlag;
    private final String cargoName;
    private final String etsngCode;
    private final String weightTons;
    private final String latitude;
    private final String longitude;
    private final Instant fetchedAtUtc;

    public RwlOperation(
            String wagonNo,
            String stationCode,
            String stationName,
            LocalDateTime operationTimeUtc,
            String operationCode,
            String operationAsoupCode,
            String operationName,
            String operationTypeId,
            String trainIndex,
            String trainNumber,
            String fromStationCode,
            String fromStationName,
            String toStationCode,
            String toStationName,
            String loadedFlag,
            String cargoName,
            String etsngCode,
            String weightTons,
            String latitude,
            String longitude,
            Instant fetchedAtUtc) {
        this.wagonNo = nullToEmpty(wagonNo);
        this.stationCode = nullToEmpty(stationCode);
        this.stationName = nullToEmpty(stationName);
        this.operationTimeUtc = operationTimeUtc;
        this.operationCode = nullToEmpty(operationCode);
        this.operationAsoupCode = nullToEmpty(operationAsoupCode);
        this.operationName = nullToEmpty(operationName);
        this.operationTypeId = nullToEmpty(operationTypeId);
        this.trainIndex = nullToEmpty(trainIndex);
        this.trainNumber = nullToEmpty(trainNumber);
        this.fromStationCode = nullToEmpty(fromStationCode);
        this.fromStationName = nullToEmpty(fromStationName);
        this.toStationCode = nullToEmpty(toStationCode);
        this.toStationName = nullToEmpty(toStationName);
        this.loadedFlag = nullToEmpty(loadedFlag);
        this.cargoName = nullToEmpty(cargoName);
        this.etsngCode = nullToEmpty(etsngCode);
        this.weightTons = nullToEmpty(weightTons);
        this.latitude = nullToEmpty(latitude);
        this.longitude = nullToEmpty(longitude);
        this.fetchedAtUtc = Objects.requireNonNull(fetchedAtUtc, "fetchedAtUtc");
    }

    public Optional<String> eventKey() {
        if (wagonNo.isEmpty() || operationTimeUtc == null || stationCode.isEmpty()) {
            return Optional.empty();
        }
        // asoup code preferred; fall back to operation_code so key remains usable when asoup missing
        String opPart = !operationAsoupCode.isEmpty() ? operationAsoupCode : operationCode;
        if (opPart.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(
                "|",
                wagonNo,
                operationTimeUtc.toString(),
                stationCode,
                opPart,
                trainIndex));
    }

    public String contentHash() {
        String normalized = String.join(
                "\n",
                wagonNo,
                stationCode,
                stationName,
                operationTimeUtc == null ? "" : operationTimeUtc.toString(),
                operationCode,
                operationAsoupCode,
                operationName,
                operationTypeId,
                trainIndex,
                trainNumber,
                fromStationCode,
                fromStationName,
                toStationCode,
                toStationName,
                loadedFlag,
                cargoName,
                etsngCode,
                weightTons,
                latitude,
                longitude);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public String missingIdentityReason() {
        if (wagonNo.isEmpty()) {
            return "missing_wagon_no";
        }
        if (operationTimeUtc == null) {
            return "missing_utc_datetime";
        }
        if (stationCode.isEmpty()) {
            return "missing_station_code";
        }
        if (operationAsoupCode.isEmpty() && operationCode.isEmpty()) {
            return "missing_operation_code";
        }
        return "incomplete_event_key";
    }

    public Instant operationInstantUtc() {
        return operationTimeUtc == null ? null : operationTimeUtc.toInstant(ZoneOffset.UTC);
    }

    public String wagonNo() {
        return wagonNo;
    }

    public String stationCode() {
        return stationCode;
    }

    public String stationName() {
        return stationName;
    }

    public LocalDateTime operationTimeUtc() {
        return operationTimeUtc;
    }

    public String operationCode() {
        return operationCode;
    }

    public String operationAsoupCode() {
        return operationAsoupCode;
    }

    public String operationName() {
        return operationName;
    }

    public String operationTypeId() {
        return operationTypeId;
    }

    public String trainIndex() {
        return trainIndex;
    }

    public String trainNumber() {
        return trainNumber;
    }

    public String fromStationCode() {
        return fromStationCode;
    }

    public String fromStationName() {
        return fromStationName;
    }

    public String toStationCode() {
        return toStationCode;
    }

    public String toStationName() {
        return toStationName;
    }

    public String loadedFlag() {
        return loadedFlag;
    }

    public String cargoName() {
        return cargoName;
    }

    public String etsngCode() {
        return etsngCode;
    }

    public String weightTons() {
        return weightTons;
    }

    public String latitude() {
        return latitude;
    }

    public String longitude() {
        return longitude;
    }

    public Instant fetchedAtUtc() {
        return fetchedAtUtc;
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v.trim();
    }
}
