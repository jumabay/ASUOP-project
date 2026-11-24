package platform.oracle.jdbc;

/**
 * Resolves A1 operationCode as VARCHAR text. IDCODE wins; CODE is string fallback only.
 * Never parses either column as a number (preserves leading zeros such as {@code 04}).
 */
public final class DbtcOperationCode {

    private DbtcOperationCode() {}

    public static String resolve(String idCode, String code) {
        String primary = trimToNull(idCode);
        if (primary != null) {
            return primary;
        }
        String fallback = trimToNull(code);
        return fallback == null ? "" : fallback;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
