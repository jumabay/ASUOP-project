package platform.disl.parser;

/**
 * Parses one KZ dislocation line.
 * Contract: tab separator; indices 0..19 required; containers only when field count is exactly 21.
 * Matches C# {@code SaveStateReportLine}: short lines fail; no station/operation uniqueness filter.
 */
public final class KzLineParser {

    public static final String NO_OWNER_INFO = "нет информации";
    private static final char FIELD_SEPARATOR = '\t';
    private static final int MIN_FIELDS = 20;
    private static final int CONTAINERS_FIELD_COUNT = 21;

    /**
     * @param line raw source line (may be empty)
     * @return parsed line
     * @throws IllegalArgumentException when structure or wagonOwner conversion fails
     */
    public KzDislocationLine parse(String line) {
        if (line == null) {
            throw new IllegalArgumentException("line is null");
        }

        String[] fields = splitPreserveAll(line);
        if (fields.length < MIN_FIELDS) {
            throw new IllegalArgumentException(
                    "expected at least " + MIN_FIELDS + " tab-separated fields, got " + fields.length);
        }

        int wagonOwner = parseWagonOwner(fields[10]);
        String containers = fields.length == CONTAINERS_FIELD_COUNT ? fields[20] : null;

        return new KzDislocationLine(
                fields[0],
                fields[1],
                fields[2],
                fields[3],
                fields[4],
                fields[5],
                fields[6],
                fields[7],
                fields[8],
                fields[9],
                wagonOwner,
                fields[11],
                fields[12],
                fields[13],
                fields[14].trim(),
                fields[15],
                fields[16],
                fields[17],
                fields[18],
                fields[19],
                containers,
                fields.length,
                line);
    }

    private static int parseWagonOwner(String raw) {
        if (NO_OWNER_INFO.equals(raw)) {
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("wagonOwner is not an integer: '" + raw + "'", ex);
        }
    }

    /** Same idea as {@code String.Split('\t')} in C#: empty trailing segments preserved. */
    private static String[] splitPreserveAll(String line) {
        return line.split(String.valueOf(FIELD_SEPARATOR), -1);
    }
}
