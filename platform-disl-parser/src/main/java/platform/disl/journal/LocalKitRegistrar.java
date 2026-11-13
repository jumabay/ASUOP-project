package platform.disl.journal;

import java.util.Objects;
import java.util.Optional;
import platform.disl.parser.KzFileParseResult;
import platform.disl.parser.KzFileParser;

/**
 * Ties KZ parse result to the local hour journal.
 * Empty file does not create a kit. Repeat of the same name+type does not create a second kit.
 * Hour and channel are supplied by the caller (R0 decision A).
 */
public final class LocalKitRegistrar {

    private final KzFileParser fileParser;
    private final LocalHourJournal journal;

    public LocalKitRegistrar(LocalHourJournal journal) {
        this(new KzFileParser(), journal);
    }

    public LocalKitRegistrar(KzFileParser fileParser, LocalHourJournal journal) {
        this.fileParser = Objects.requireNonNull(fileParser, "fileParser");
        this.journal = Objects.requireNonNull(journal, "journal");
    }

    public LocalHourJournal journal() {
        return journal;
    }

    public KitRegistrationResult registerContent(
            String fileName,
            int type,
            ChannelId channelId,
            HourSlot hourSlot,
            String content) {
        KzFileParseResult parsed = fileParser.parseContent(content);
        return registerParsed(fileName, type, channelId, hourSlot, parsed);
    }

    public KitRegistrationResult registerParsed(
            String fileName,
            int type,
            ChannelId channelId,
            HourSlot hourSlot,
            KzFileParseResult parsed) {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(hourSlot, "hourSlot");
        Objects.requireNonNull(parsed, "parsed");

        if (parsed.isEmptyFile()) {
            return KitRegistrationResult.skippedEmpty(parsed);
        }

        SliceKitKey key = new SliceKitKey(fileName, type);
        SliceKit candidate = new SliceKit(key, channelId, hourSlot, parsed.lines().size());
        Optional<SliceKit> registered = journal.tryRegister(candidate);
        if (registered.isEmpty()) {
            return KitRegistrationResult.duplicateSkipped(parsed, candidate);
        }
        return KitRegistrationResult.registered(parsed, registered.get());
    }
}
