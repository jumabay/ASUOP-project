package platform.rwl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import platform.intake.IntakeReject;
import platform.intake.NormalizedDislocationEvent;
import platform.rwl.parse.RawRwlOperation;

/**
 * Maps raw RWL operations to source-neutral intake events.
 * eventKey formula is a working hypothesis (not an RWL contract).
 */
public final class RwlNormalizer {

    public static final String SOURCE_RWL = "RWL";

    public record NormalizationOutcome(
            List<NormalizedDislocationEvent> events, List<IntakeReject> rejects) {}

    public NormalizationOutcome normalize(List<RawRwlOperation> operations, List<IntakeReject> priorRejects) {
        List<NormalizedDislocationEvent> events = new ArrayList<>();
        List<IntakeReject> rejects = new ArrayList<>(priorRejects);
        for (RawRwlOperation op : operations) {
            Optional<String> key = eventKey(op);
            if (key.isEmpty()) {
                rejects.add(new IntakeReject(missingReason(op), op.wagonNo, "incomplete identity"));
                continue;
            }
            events.add(toEvent(op, key.get()));
        }
        return new NormalizationOutcome(List.copyOf(events), List.copyOf(rejects));
    }

    private NormalizedDislocationEvent toEvent(RawRwlOperation op, String key) {
        return new NormalizedDislocationEvent(
                op.wagonNo,
                op.stationCode,
                op.stationName,
                op.operationTimeUtc == null ? null : op.operationTimeUtc.toInstant(ZoneOffset.UTC),
                op.operationCode,
                op.operationAsoupCode,
                op.operationName,
                op.trainIndex,
                op.trainNumber,
                op.fromStationCode,
                op.toStationCode,
                op.loadedFlag,
                op.cargoName,
                op.etsngCode,
                op.weightTons,
                key,
                contentHash(op),
                SOURCE_RWL,
                op.fetchedAtUtc);
    }

    static Optional<String> eventKey(RawRwlOperation op) {
        if (op.wagonNo == null || op.wagonNo.isBlank()) {
            return Optional.empty();
        }
        if (op.operationTimeUtc == null) {
            return Optional.empty();
        }
        if (op.stationCode == null || op.stationCode.isBlank()) {
            return Optional.empty();
        }
        String opPart = !isBlank(op.operationAsoupCode) ? op.operationAsoupCode : op.operationCode;
        if (isBlank(opPart)) {
            return Optional.empty();
        }
        return Optional.of(String.join(
                "|",
                op.wagonNo.trim(),
                op.operationTimeUtc.toString(),
                op.stationCode.trim(),
                opPart.trim(),
                op.trainIndex == null ? "" : op.trainIndex.trim()));
    }

    static String contentHash(RawRwlOperation op) {
        String normalized = String.join(
                "\n",
                nullToEmpty(op.wagonNo),
                nullToEmpty(op.stationCode),
                nullToEmpty(op.stationName),
                op.operationTimeUtc == null ? "" : op.operationTimeUtc.toString(),
                nullToEmpty(op.operationCode),
                nullToEmpty(op.operationAsoupCode),
                nullToEmpty(op.operationName),
                nullToEmpty(op.trainIndex),
                nullToEmpty(op.trainNumber),
                nullToEmpty(op.fromStationCode),
                nullToEmpty(op.toStationCode),
                nullToEmpty(op.loadedFlag),
                nullToEmpty(op.cargoName),
                nullToEmpty(op.etsngCode),
                nullToEmpty(op.weightTons));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String missingReason(RawRwlOperation op) {
        if (isBlank(op.wagonNo)) {
            return "missing_wagon_no";
        }
        if (op.operationTimeUtc == null) {
            return "missing_utc_datetime";
        }
        if (isBlank(op.stationCode)) {
            return "missing_station_code";
        }
        return "missing_operation_code";
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
