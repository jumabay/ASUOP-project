package platform.oracle.dict.semantics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Synthetic model for RAIL_DISL_EVENT provenance (P3C-0). No Oracle I/O.
 *
 * <p>Policy: every migrated tape row gets a technical event id; duplicates are retained;
 * natural keys from DBTC are evidence only, never assumed unique.
 */
public final class DislEventProvenance {

    public record TapeRowRef(
            long legacyReportId,
            long legacyWagonId,
            String gettingDateIso,
            String locationDateIso,
            Long locationStationId,
            Long operationId,
            Long currentStateId,
            String trainCode,
            int sourceOrdinalInReport) {}

    private DislEventProvenance() {}

    /** Stable content fingerprint for dedup *detection* (not deletion). */
    public static String contentHashSha256(TapeRowRef row) {
        Objects.requireNonNull(row, "row");
        String payload = String.join(
                "|",
                Long.toString(row.legacyReportId()),
                Long.toString(row.legacyWagonId()),
                nullToEmpty(row.gettingDateIso()),
                nullToEmpty(row.locationDateIso()),
                String.valueOf(row.locationStationId()),
                String.valueOf(row.operationId()),
                String.valueOf(row.currentStateId()),
                nullToEmpty(row.trainCode()));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Soft business key candidate — NOT unique in DBTC. Used only as migration watermark aid.
     * Format: reportId:wagonId:ordinal
     */
    public static String provenanceKey(TapeRowRef row) {
        return row.legacyReportId() + ":" + row.legacyWagonId() + ":" + row.sourceOrdinalInReport();
    }

    /** True when payload fields differ (analytics). Never used to drop rows. */
    public static boolean contentDiffers(TapeRowRef a, TapeRowRef b) {
        return !contentHashSha256(a).equals(contentHashSha256(b));
    }

    /**
     * Migration policy: every distinct source row is retained (own technical PK + provenanceKey).
     * Identical content hashes are still kept as separate events.
     */
    public static boolean retainSourceRow(TapeRowRef row) {
        return row != null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
