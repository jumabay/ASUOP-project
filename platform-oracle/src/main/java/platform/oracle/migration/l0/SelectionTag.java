package platform.oracle.migration.l0;

/**
 * Migration selection tags from docs 25/29/44.
 * Watched membership uses {@link #SKIP} while G31 remains OPEN.
 */
public enum SelectionTag {
    REF_FULL,
    FULL,
    OPEN,
    TWELVE_M("12M"),
    BOOTSTRAP,
    ARCHIVE_DBTC,
    SKIP;

    private final String wireName;

    SelectionTag() {
        this.wireName = name();
    }

    SelectionTag(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static SelectionTag fromWire(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("selection tag required");
        }
        String normalized = value.trim();
        if ("12M".equalsIgnoreCase(normalized)) {
            return TWELVE_M;
        }
        return SelectionTag.valueOf(normalized.toUpperCase());
    }
}
