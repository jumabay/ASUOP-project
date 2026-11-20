package platform.oracle.migration.l0;

import platform.oracle.migration.l0.model.TapeEventRecord;

/**
 * Tape identity helpers. Fixture pack already supplies stable {@code event_id} /
 * {@code provenance_key}; engine preserves them (NK1 OPEN — no UK collapse).
 */
public final class TapeIdentity {

    private TapeIdentity() {
    }

    public static String provenanceKey(TapeEventRecord event) {
        if (event.provenanceKey() != null && !event.provenanceKey().isBlank()) {
            return event.provenanceKey();
        }
        return "SYN|TAPE|" + event.code() + "|" + event.gettingTs() + "|" + event.eventId();
    }

    public static long eventId(TapeEventRecord event) {
        return event.eventId();
    }
}
