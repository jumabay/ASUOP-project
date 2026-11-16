package platform.intake;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Source-neutral dislocation event after normalization (RWL, FTP, …).
 * Domain modules must not depend on RWL XML types.
 */
public final class NormalizedDislocationEvent {

    private final String wagonCode;
    private final String stationCode;
    private final String stationName;
    private final Instant operationTimeUtc;
    private final String operationCode;
    private final String operationAsoupCode;
    private final String operationName;
    private final String trainIndex;
    private final String trainNumber;
    private final String fromStationCode;
    private final String toStationCode;
    private final String loadedFlag;
    private final String cargoName;
    private final String etsngCode;
    private final String weightTons;
    private final String eventKey;
    private final String contentHash;
    private final String sourceCode;
    private final Instant fetchedAtUtc;

    public NormalizedDislocationEvent(
            String wagonCode,
            String stationCode,
            String stationName,
            Instant operationTimeUtc,
            String operationCode,
            String operationAsoupCode,
            String operationName,
            String trainIndex,
            String trainNumber,
            String fromStationCode,
            String toStationCode,
            String loadedFlag,
            String cargoName,
            String etsngCode,
            String weightTons,
            String eventKey,
            String contentHash,
            String sourceCode,
            Instant fetchedAtUtc) {
        this.wagonCode = Objects.requireNonNull(wagonCode, "wagonCode");
        this.stationCode = nullToEmpty(stationCode);
        this.stationName = nullToEmpty(stationName);
        this.operationTimeUtc = operationTimeUtc;
        this.operationCode = nullToEmpty(operationCode);
        this.operationAsoupCode = nullToEmpty(operationAsoupCode);
        this.operationName = nullToEmpty(operationName);
        this.trainIndex = nullToEmpty(trainIndex);
        this.trainNumber = nullToEmpty(trainNumber);
        this.fromStationCode = nullToEmpty(fromStationCode);
        this.toStationCode = nullToEmpty(toStationCode);
        this.loadedFlag = nullToEmpty(loadedFlag);
        this.cargoName = nullToEmpty(cargoName);
        this.etsngCode = nullToEmpty(etsngCode);
        this.weightTons = nullToEmpty(weightTons);
        this.eventKey = Objects.requireNonNull(eventKey, "eventKey");
        this.contentHash = Objects.requireNonNull(contentHash, "contentHash");
        this.sourceCode = Objects.requireNonNull(sourceCode, "sourceCode");
        this.fetchedAtUtc = Objects.requireNonNull(fetchedAtUtc, "fetchedAtUtc");
    }

    public Optional<Instant> operationTimeUtc() {
        return Optional.ofNullable(operationTimeUtc);
    }

    public String wagonCode() {
        return wagonCode;
    }

    public String stationCode() {
        return stationCode;
    }

    public String stationName() {
        return stationName;
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

    public String trainIndex() {
        return trainIndex;
    }

    public String trainNumber() {
        return trainNumber;
    }

    public String fromStationCode() {
        return fromStationCode;
    }

    public String toStationCode() {
        return toStationCode;
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

    public String eventKey() {
        return eventKey;
    }

    public String contentHash() {
        return contentHash;
    }

    public String sourceCode() {
        return sourceCode;
    }

    public Instant fetchedAtUtc() {
        return fetchedAtUtc;
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v.trim();
    }
}
