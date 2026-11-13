package platform.disl.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KzLineParserTest {

    private final KzLineParser parser = new KzLineParser();

    @Test
    void parsesTwentyFieldLineWithoutContainers() {
        KzDislocationLine line = parser.parse(SyntheticKzLines.line20("11111111", "700001", "3"));

        assertEquals(20, line.fieldCount());
        assertEquals("11111111", line.wagonCode());
        assertEquals("700001", line.dislocationStationCode());
        assertEquals("3", line.operationCode());
        assertEquals(0, line.wagonOwner());
        assertEquals("doc", line.docNumber());
        assertTrue(line.containers().isEmpty());
    }

    @Test
    void parsesTwentyOneFieldLineWithContainers() {
        KzDislocationLine line =
                parser.parse(SyntheticKzLines.line21("22222222", "700002", "4", "CONT1;CONT2"));

        assertEquals(21, line.fieldCount());
        assertEquals("22222222", line.wagonCode());
        assertTrue(line.containers().isPresent());
        assertEquals("CONT1;CONT2", line.containers().orElseThrow());
    }

    @Test
    void parsesNumericWagonOwner() {
        String raw = SyntheticKzLines.line20("33333333", "700003", "5").replace("нет информации", "42");
        assertEquals(42, parser.parse(raw).wagonOwner());
    }

    @Test
    void rejectsStructurallyShortLine() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(SyntheticKzLines.tooShort()));
    }

    @Test
    void rejectsInvalidWagonOwner() {
        String raw = SyntheticKzLines.line20("44444444", "700004", "6").replace("нет информации", "abc");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(raw));
    }
}
