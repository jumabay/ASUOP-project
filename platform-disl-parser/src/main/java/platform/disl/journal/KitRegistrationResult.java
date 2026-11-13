package platform.disl.journal;

import java.util.Objects;
import java.util.Optional;
import platform.disl.parser.KzFileParseResult;

/** Outcome of parsing a file and optionally registering a kit in the local journal. */
public final class KitRegistrationResult {

    public enum Status {
        /** Empty source: no kit created. */
        SKIPPED_EMPTY,
        /** Kit registered. */
        REGISTERED,
        /** Same file name + type already present; no second kit. */
        DUPLICATE_SKIPPED
    }

    private final Status status;
    private final KzFileParseResult parseResult;
    private final SliceKit kit;

    private KitRegistrationResult(Status status, KzFileParseResult parseResult, SliceKit kit) {
        this.status = Objects.requireNonNull(status, "status");
        this.parseResult = Objects.requireNonNull(parseResult, "parseResult");
        this.kit = kit;
    }

    public static KitRegistrationResult skippedEmpty(KzFileParseResult parseResult) {
        return new KitRegistrationResult(Status.SKIPPED_EMPTY, parseResult, null);
    }

    public static KitRegistrationResult registered(KzFileParseResult parseResult, SliceKit kit) {
        return new KitRegistrationResult(Status.REGISTERED, parseResult, Objects.requireNonNull(kit, "kit"));
    }

    public static KitRegistrationResult duplicateSkipped(KzFileParseResult parseResult, SliceKit existingIgnored) {
        return new KitRegistrationResult(Status.DUPLICATE_SKIPPED, parseResult, null);
    }

    public Status status() {
        return status;
    }

    public KzFileParseResult parseResult() {
        return parseResult;
    }

    public Optional<SliceKit> kit() {
        return Optional.ofNullable(kit);
    }
}
