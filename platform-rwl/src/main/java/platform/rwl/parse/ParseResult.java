package platform.rwl.parse;

import java.util.ArrayList;
import java.util.List;
import platform.intake.IntakeReject;

public final class ParseResult {

    private final ParseStatus status;
    private final String errorCode;
    private final List<RawRwlOperation> operations;
    private final List<IntakeReject> rejects;
    private final int wagonsSeen;

    public ParseResult(
            ParseStatus status,
            String errorCode,
            List<RawRwlOperation> operations,
            List<IntakeReject> rejects,
            int wagonsSeen) {
        this.status = status;
        this.errorCode = errorCode == null ? "" : errorCode;
        this.operations = List.copyOf(operations);
        this.rejects = List.copyOf(rejects);
        this.wagonsSeen = wagonsSeen;
    }

    public static ParseResult apiError(String code) {
        return new ParseResult(ParseStatus.API_ERROR, code, List.of(), List.of(), 0);
    }

    public static ParseResult parseError(String code) {
        return new ParseResult(ParseStatus.PARSE_ERROR, code, List.of(), List.of(), 0);
    }

    public ParseStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    public List<RawRwlOperation> operations() {
        return operations;
    }

    public List<IntakeReject> rejects() {
        return rejects;
    }

    public int wagonsSeen() {
        return wagonsSeen;
    }

    public static final class Builder {
        private final List<RawRwlOperation> operations = new ArrayList<>();
        private final List<IntakeReject> rejects = new ArrayList<>();
        private int wagonsSeen;

        public void addWagon() {
            wagonsSeen++;
        }

        public void addOperation(RawRwlOperation op) {
            operations.add(op);
        }

        public void addReject(IntakeReject reject) {
            rejects.add(reject);
        }

        public ParseResult buildOkOrEmpty() {
            if (operations.isEmpty() && rejects.isEmpty()) {
                return new ParseResult(ParseStatus.EMPTY, "", operations, rejects, wagonsSeen);
            }
            return new ParseResult(ParseStatus.OK, "", operations, rejects, wagonsSeen);
        }
    }
}
