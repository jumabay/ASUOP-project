package platform.disl.parser;

import java.time.LocalDateTime;
import platform.disl.journal.ChannelId;
import platform.disl.journal.HourSlot;
import platform.disl.journal.LocalHourJournal;
import platform.disl.journal.LocalKitRegistrar;

/** Test helper to register one synthetic kit. */
public final class KitRegistrationSupport {

    private KitRegistrationSupport() {
    }

    public static void register(
            LocalHourJournal journal,
            String fileName,
            int type,
            ChannelId channelId,
            LocalDateTime hour) {
        new LocalKitRegistrar(journal)
                .registerContent(fileName, type, channelId, HourSlot.of(hour), SyntheticSupport.twoSameStationLines());
    }
}
