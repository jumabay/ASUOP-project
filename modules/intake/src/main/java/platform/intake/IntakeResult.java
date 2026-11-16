package platform.intake;

import java.util.Objects;

public final class IntakeResult {

    public enum Status {
        ACCEPTED,
        DUPLICATE,
        REJECTED
    }

    private final Status status;
    private final String intakeKey;
    private final int eventCount;
    private final int rejectCount;

    public IntakeResult(Status status, String intakeKey, int eventCount, int rejectCount) {
        this.status = Objects.requireNonNull(status, "status");
        this.intakeKey = Objects.requireNonNull(intakeKey, "intakeKey");
        this.eventCount = eventCount;
        this.rejectCount = rejectCount;
    }

    public Status status() {
        return status;
    }

    public String intakeKey() {
        return intakeKey;
    }

    public int eventCount() {
        return eventCount;
    }

    public int rejectCount() {
        return rejectCount;
    }
}
