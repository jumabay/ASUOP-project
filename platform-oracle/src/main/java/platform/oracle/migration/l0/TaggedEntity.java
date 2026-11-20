package platform.oracle.migration.l0;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * One golden-comparable tagged row (mirrors {@code expected_selection.json} entries).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaggedEntity(
        Long id,
        String code,
        String filename,
        Integer type,
        LocalDateTime gettingTs,
        SelectionTag tag,
        String exclusionReason,
        String provenanceKey
) {
    public static TaggedEntity ref(long id, String code) {
        return new TaggedEntity(id, code, null, null, null, SelectionTag.REF_FULL, null, null);
    }

    public static TaggedEntity of(long id, String code, SelectionTag tag, String exclusionReason) {
        return new TaggedEntity(id, code, null, null, null, tag, exclusionReason, null);
    }

    public static TaggedEntity kit(long kitId, String filename, int type, SelectionTag tag, String exclusionReason) {
        return new TaggedEntity(kitId, null, filename, type, null, tag, exclusionReason, null);
    }

    public static TaggedEntity tape(
            long eventId,
            String code,
            LocalDateTime gettingTs,
            SelectionTag tag,
            String exclusionReason,
            String provenanceKey) {
        return new TaggedEntity(eventId, code, null, null, gettingTs, tag, exclusionReason, provenanceKey);
    }

    public String tagWire() {
        return tag.wireName();
    }
}
