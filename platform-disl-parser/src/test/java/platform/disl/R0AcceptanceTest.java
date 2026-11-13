package platform.disl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import platform.disl.journal.ChannelHourCalendar;
import platform.disl.journal.ChannelId;
import platform.disl.journal.HourSlot;
import platform.disl.journal.KitRegistrationResult;
import platform.disl.journal.LocalHourJournal;
import platform.disl.journal.LocalKitRegistrar;
import platform.disl.parser.KzDislocationLine;
import platform.disl.parser.KzFileParseResult;
import platform.disl.parser.KzFileParser;
import platform.disl.parser.KzLineParser;
import platform.disl.parser.SyntheticSupport;
import platform.disl.parser.SyntheticKzLines;

/**
 * R0 checks from docs/architecture/16_TESTING_STRATEGY.md (archive §7 meaning).
 * Synthetic data only — not claimed to match industrial disl_kz*.txt.
 */
class R0AcceptanceTest {

    private static final ChannelId KZ = new ChannelId("KZ");
    private static final int TYPE_KZ = 0;

    @Test
    void twentyAndTwentyOneFields_containersOnlyOnTwentyOne() {
        KzLineParser parser = new KzLineParser();
        KzDislocationLine line20 = parser.parse(SyntheticKzLines.line20("11111111", "700001", "3"));
        KzDislocationLine line21 =
                parser.parse(SyntheticKzLines.line21("22222222", "700002", "4", "CONT-A"));

        assertEquals(20, line20.fieldCount());
        assertTrue(line20.containers().isEmpty());
        assertEquals(21, line21.fieldCount());
        assertEquals("CONT-A", line21.containers().orElseThrow());
    }

    @Test
    void emptyFile_journalReceivesNoKit() {
        LocalKitRegistrar registrar = new LocalKitRegistrar(new LocalHourJournal());
        KitRegistrationResult result = registrar.registerContent(
                "disl_kz_empty.txt",
                TYPE_KZ,
                KZ,
                HourSlot.of(LocalDateTime.of(2026, 3, 1, 8, 0)),
                "");
        assertEquals(KitRegistrationResult.Status.SKIPPED_EMPTY, result.status());
        assertEquals(0, registrar.journal().size());
    }

    @Test
    void twoLinesSameStation_bothRemainInHour() {
        KzFileParseResult parsed = new KzFileParser().parseContent(SyntheticSupport.twoSameStationLines());
        assertEquals(2, parsed.lines().size());
        assertEquals("700001", parsed.lines().get(0).dislocationStationCode());
        assertEquals("700001", parsed.lines().get(1).dislocationStationCode());
        assertEquals("3", parsed.lines().get(0).operationCode());
        assertEquals("3", parsed.lines().get(1).operationCode());
        assertFalse(parsed.lines().get(0).wagonCode().equals(parsed.lines().get(1).wagonCode()));
    }

    @Test
    void repeatNameAndType_secondKitNotCreated() {
        LocalKitRegistrar registrar = new LocalKitRegistrar(new LocalHourJournal());
        HourSlot hour = HourSlot.of(LocalDateTime.of(2026, 3, 1, 9, 0));
        String body = SyntheticSupport.twoSameStationLines();

        assertEquals(
                KitRegistrationResult.Status.REGISTERED,
                registrar.registerContent("disl_kz_same.txt", TYPE_KZ, KZ, hour, body).status());
        assertEquals(
                KitRegistrationResult.Status.DUPLICATE_SKIPPED,
                registrar.registerContent("disl_kz_same.txt", TYPE_KZ, KZ, hour, body).status());
        assertEquals(1, registrar.journal().size());
    }

    @Test
    void calendarHole_missedHourListed() {
        LocalHourJournal journal = new LocalHourJournal();
        LocalKitRegistrar registrar = new LocalKitRegistrar(journal);
        HourSlot h08 = HourSlot.of(LocalDateTime.of(2026, 3, 1, 8, 0));
        HourSlot h09 = HourSlot.of(LocalDateTime.of(2026, 3, 1, 9, 0));
        HourSlot h10 = HourSlot.of(LocalDateTime.of(2026, 3, 1, 10, 0));

        registrar.registerContent("f08.txt", TYPE_KZ, KZ, h08, SyntheticSupport.twoSameStationLines());
        registrar.registerContent("f10.txt", TYPE_KZ, KZ, h10, SyntheticSupport.twoSameStationLines());

        List<HourSlot> missing = new ChannelHourCalendar(journal).findMissingInRange(KZ, h08, h10);
        assertEquals(List.of(h09), missing);
    }

    @Test
    void incorrectLine_isReportedAndDoesNotStopOthers() {
        KzFileParseResult result = new KzFileParser()
                .parseLines(List.of(
                        SyntheticKzLines.line20("1", "S", "1"),
                        SyntheticKzLines.tooShort(),
                        SyntheticKzLines.line21("2", "S", "1", "C")));
        assertEquals(2, result.lines().size());
        assertEquals(1, result.errors().size());
        assertEquals(2, result.errors().get(0).lineNumber());
    }
}
