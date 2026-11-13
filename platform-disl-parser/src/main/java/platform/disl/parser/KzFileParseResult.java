package platform.disl.parser;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of parsing all lines of a KZ file.
 * Successful lines keep source order. Invalid lines are collected and do not remove valid ones.
 */
public final class KzFileParseResult {

    private final List<KzDislocationLine> lines;
    private final List<LineParseError> errors;
    private final boolean emptyFile;

    public KzFileParseResult(List<KzDislocationLine> lines, List<LineParseError> errors, boolean emptyFile) {
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        this.emptyFile = emptyFile;
    }

    public List<KzDislocationLine> lines() {
        return lines;
    }

    public List<LineParseError> errors() {
        return errors;
    }

    /** True when the source had no content (zero-length / no lines), as with C# empty-file skip. */
    public boolean isEmptyFile() {
        return emptyFile;
    }

    public static KzFileParseResult emptyFile() {
        return new KzFileParseResult(Collections.emptyList(), Collections.emptyList(), true);
    }
}
