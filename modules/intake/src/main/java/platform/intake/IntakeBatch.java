package platform.intake;

import java.util.List;
import java.util.Objects;

/** One accepted intake payload (already normalized). */
public final class IntakeBatch {

    private final String intakeKey;
    private final String sourceType;
    private final String requestSummary;
    private final List<NormalizedDislocationEvent> events;
    private final List<IntakeReject> rejects;

    public IntakeBatch(
            String intakeKey,
            String sourceType,
            String requestSummary,
            List<NormalizedDislocationEvent> events,
            List<IntakeReject> rejects) {
        this.intakeKey = Objects.requireNonNull(intakeKey, "intakeKey");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.requestSummary = requestSummary == null ? "" : requestSummary;
        this.events = List.copyOf(Objects.requireNonNull(events, "events"));
        this.rejects = List.copyOf(Objects.requireNonNull(rejects, "rejects"));
    }

    public String intakeKey() {
        return intakeKey;
    }

    public String sourceType() {
        return sourceType;
    }

    public String requestSummary() {
        return requestSummary;
    }

    public List<NormalizedDislocationEvent> events() {
        return events;
    }

    public List<IntakeReject> rejects() {
        return rejects;
    }
}
