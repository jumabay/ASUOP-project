package platform.rwl.proto.parse;

import java.util.ArrayList;
import java.util.List;
import platform.rwl.proto.model.RwlOperation;
import platform.rwl.proto.model.RunStatus;

public final class ParseOutcome {

    private final RunStatus status;
    private final List<RwlOperation> operations;
    private final List<RejectedItem> rejects;
    private final String errorSummary;
    private final int wagonsSeen;

    private ParseOutcome(
            RunStatus status,
            List<RwlOperation> operations,
            List<RejectedItem> rejects,
            String errorSummary,
            int wagonsSeen) {
        this.status = status;
        this.operations = List.copyOf(operations);
        this.rejects = List.copyOf(rejects);
        this.errorSummary = errorSummary;
        this.wagonsSeen = wagonsSeen;
    }

    public static ParseOutcome success(List<RwlOperation> ops, List<RejectedItem> rejects, int wagonsSeen) {
        if (ops.isEmpty() && rejects.isEmpty()) {
            return new ParseOutcome(RunStatus.EMPTY, List.of(), List.of(), null, wagonsSeen);
        }
        return new ParseOutcome(RunStatus.SUCCESS, ops, rejects, null, wagonsSeen);
    }

    public static ParseOutcome apiError(String summary) {
        return new ParseOutcome(RunStatus.API_ERROR, List.of(), List.of(), summary, 0);
    }

    public static ParseOutcome parseError(String summary) {
        return new ParseOutcome(RunStatus.PARSE_ERROR, List.of(), List.of(), summary, 0);
    }

    public RunStatus status() {
        return status;
    }

    public List<RwlOperation> operations() {
        return operations;
    }

    public List<RejectedItem> rejects() {
        return rejects;
    }

    public String errorSummary() {
        return errorSummary;
    }

    public int wagonsSeen() {
        return wagonsSeen;
    }

    public record RejectedItem(String wagonNo, String reasonCode, String detail, String rawExcerpt) {
    }

    public static final class Builder {
        private final List<RwlOperation> operations = new ArrayList<>();
        private final List<RejectedItem> rejects = new ArrayList<>();
        private int wagonsSeen;

        public void addWagon() {
            wagonsSeen++;
        }

        public void addOperation(RwlOperation op) {
            operations.add(op);
        }

        public void addReject(String wagonNo, String reason, String detail, String excerpt) {
            rejects.add(new RejectedItem(wagonNo, reason, detail, excerpt));
        }

        public ParseOutcome buildSuccessOrEmpty() {
            return ParseOutcome.success(operations, rejects, wagonsSeen);
        }
    }
}
