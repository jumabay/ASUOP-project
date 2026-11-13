package platform.disl.parser;

import java.util.Objects;

/** A source line that could not be mapped to {@link KzDislocationLine}. */
public final class LineParseError {

    private final int lineNumber;
    private final String rawLine;
    private final String reason;

    public LineParseError(int lineNumber, String rawLine, String reason) {
        this.lineNumber = lineNumber;
        this.rawLine = Objects.requireNonNull(rawLine, "rawLine");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String rawLine() {
        return rawLine;
    }

    public String reason() {
        return reason;
    }
}
