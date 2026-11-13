package platform.disl.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses a whole KZ text file (synthetic or local content). Does not touch Oracle or the network.
 * Every structurally valid line is kept; station+operation are not used as a unique key.
 */
public final class KzFileParser {

    private final KzLineParser lineParser;

    public KzFileParser() {
        this(new KzLineParser());
    }

    public KzFileParser(KzLineParser lineParser) {
        this.lineParser = Objects.requireNonNull(lineParser, "lineParser");
    }

    /**
     * @param content full file text; {@code null} or empty string is an empty file
     */
    public KzFileParseResult parseContent(String content) {
        if (content == null || content.isEmpty()) {
            return KzFileParseResult.emptyFile();
        }
        return parseLines(splitLines(content));
    }

    /**
     * @param lines already split lines (e.g. from a test fixture); empty list is an empty file
     */
    public KzFileParseResult parseLines(List<String> lines) {
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            return KzFileParseResult.emptyFile();
        }

        List<KzDislocationLine> parsed = new ArrayList<>();
        List<LineParseError> errors = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            int lineNumber = i + 1;
            try {
                parsed.add(lineParser.parse(raw));
            } catch (RuntimeException ex) {
                errors.add(new LineParseError(lineNumber, raw == null ? "" : raw, ex.getMessage()));
            }
        }

        return new KzFileParseResult(parsed, errors, false);
    }

    private static List<String> splitLines(String content) {
        // Match typical ReadAllLines behaviour for \n / \r\n; do not invent exotic encodings.
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\n", -1);
        List<String> result = new ArrayList<>(parts.length);
        // Trailing empty segment after final newline is dropped, as with most ReadAllLines APIs.
        int limit = parts.length;
        if (limit > 0 && parts[limit - 1].isEmpty()) {
            limit--;
        }
        for (int i = 0; i < limit; i++) {
            result.add(parts[i]);
        }
        return result;
    }
}
