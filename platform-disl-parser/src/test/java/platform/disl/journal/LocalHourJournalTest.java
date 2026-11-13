package platform.disl.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import platform.disl.parser.KitRegistrationSupport;
import platform.disl.parser.SyntheticSupport;

class LocalHourJournalTest {

    private static final ChannelId KZ = new ChannelId("KZ");
    private static final int TYPE_KZ = 0;

    @Test
    void emptyFileDoesNotCreateKit() {
        LocalHourJournal journal = new LocalHourJournal();
        LocalKitRegistrar registrar = new LocalKitRegistrar(journal);

        KitRegistrationResult result = registrar.registerContent(
                "disl_kz_synthetic_empty.txt",
                TYPE_KZ,
                KZ,
                HourSlot.of(LocalDateTime.of(2026, 1, 1, 10, 0)),
                "");

        assertEquals(KitRegistrationResult.Status.SKIPPED_EMPTY, result.status());
        assertEquals(0, journal.size());
    }

    @Test
    void registersNonEmptyParseAsSingleKit() {
        LocalHourJournal journal = new LocalHourJournal();
        LocalKitRegistrar registrar = new LocalKitRegistrar(journal);
        HourSlot hour = HourSlot.of(LocalDateTime.of(2026, 1, 1, 10, 35));

        KitRegistrationResult result = registrar.registerContent(
                "disl_kz_synthetic_a.txt",
                TYPE_KZ,
                KZ,
                hour,
                SyntheticSupport.twoSameStationLines());

        assertEquals(KitRegistrationResult.Status.REGISTERED, result.status());
        assertEquals(1, journal.size());
        assertEquals(2, result.parseResult().lines().size());
        assertEquals(HourSlot.of(LocalDateTime.of(2026, 1, 1, 10, 0)), result.kit().orElseThrow().hourSlot());
    }

    @Test
    void repeatSameNameAndTypeDoesNotCreateSecondKit() {
        LocalHourJournal journal = new LocalHourJournal();
        LocalKitRegistrar registrar = new LocalKitRegistrar(journal);
        HourSlot hour = HourSlot.of(LocalDateTime.of(2026, 1, 1, 11, 0));
        String content = SyntheticSupport.twoSameStationLines();

        KitRegistrationResult first = registrar.registerContent(
                "disl_kz_synthetic_b.txt", TYPE_KZ, KZ, hour, content);
        KitRegistrationResult second = registrar.registerContent(
                "disl_kz_synthetic_b.txt", TYPE_KZ, KZ, hour, content);

        assertEquals(KitRegistrationResult.Status.REGISTERED, first.status());
        assertEquals(KitRegistrationResult.Status.DUPLICATE_SKIPPED, second.status());
        assertEquals(1, journal.size());
    }

    @Test
    void sameNameDifferentTypeCreatesSeparateKit() {
        LocalHourJournal journal = new LocalHourJournal();
        LocalKitRegistrar registrar = new LocalKitRegistrar(journal);
        HourSlot hour = HourSlot.of(LocalDateTime.of(2026, 1, 1, 12, 0));
        String content = SyntheticSupport.twoSameStationLines();

        registrar.registerContent("disl_kz_synthetic_c.txt", 0, KZ, hour, content);
        registrar.registerContent("disl_kz_synthetic_c.txt", 1, new ChannelId("RU"), hour, content);

        assertEquals(2, journal.size());
    }

    @Test
    void missedHourIsListedInChannelCalendar() {
        LocalHourJournal journal = new LocalHourJournal();
        LocalKitRegistrar registrar = new LocalKitRegistrar(journal);

        HourSlot h10 = HourSlot.of(LocalDateTime.of(2026, 1, 1, 10, 0));
        HourSlot h11 = HourSlot.of(LocalDateTime.of(2026, 1, 1, 11, 0));
        HourSlot h12 = HourSlot.of(LocalDateTime.of(2026, 1, 1, 12, 0));

        registrar.registerContent(
                "disl_kz_synthetic_10.txt", TYPE_KZ, KZ, h10, SyntheticSupport.twoSameStationLines());
        registrar.registerContent(
                "disl_kz_synthetic_12.txt", TYPE_KZ, KZ, h12, SyntheticSupport.twoSameStationLines());

        ChannelHourCalendar calendar = new ChannelHourCalendar(journal);
        List<HourSlot> missing = calendar.findMissingInRange(KZ, h10, h12);

        assertEquals(List.of(h11), missing);
    }

    @Test
    void calendarUsesOnlyExplicitExpectedSlots() {
        LocalHourJournal journal = new LocalHourJournal();
        KitRegistrationSupport.register(journal, "a.txt", TYPE_KZ, KZ, LocalDateTime.of(2026, 1, 1, 10, 0));

        HourSlot h09 = HourSlot.of(LocalDateTime.of(2026, 1, 1, 9, 0));
        HourSlot h10 = HourSlot.of(LocalDateTime.of(2026, 1, 1, 10, 0));
        HourSlot h11 = HourSlot.of(LocalDateTime.of(2026, 1, 1, 11, 0));

        List<HourSlot> missing = new ChannelHourCalendar(journal).findMissing(KZ, List.of(h09, h10, h11));
        assertEquals(List.of(h09, h11), missing);
        assertTrue(journal.hasKitFor(KZ, h10));
    }
}
