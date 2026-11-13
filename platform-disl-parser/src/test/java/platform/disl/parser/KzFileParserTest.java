package platform.disl.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class KzFileParserTest {

    private final KzFileParser parser = new KzFileParser();

    @Test
    void emptyContentIsEmptyFile() {
        KzFileParseResult result = parser.parseContent("");
        assertTrue(result.isEmptyFile());
        assertTrue(result.lines().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void nullContentIsEmptyFile() {
        assertTrue(parser.parseContent(null).isEmptyFile());
    }

    @Test
    void keepsBothLinesWithSameStationAndOperation() {
        String a = SyntheticKzLines.line20("11111111", "700001", "3");
        String b = SyntheticKzLines.line20("22222222", "700001", "3");

        KzFileParseResult result = parser.parseLines(List.of(a, b));

        assertFalse(result.isEmptyFile());
        assertEquals(2, result.lines().size());
        assertEquals("11111111", result.lines().get(0).wagonCode());
        assertEquals("22222222", result.lines().get(1).wagonCode());
        assertEquals("700001", result.lines().get(0).dislocationStationCode());
        assertEquals("700001", result.lines().get(1).dislocationStationCode());
        assertEquals("3", result.lines().get(0).operationCode());
        assertEquals("3", result.lines().get(1).operationCode());
    }

    @Test
    void invalidLineIsCollectedAndDoesNotDropValidNeighbours() {
        String valid20 = SyntheticKzLines.line20("11111111", "700001", "3");
        String valid21 = SyntheticKzLines.line21("22222222", "700002", "4", "C1");
        String invalid = SyntheticKzLines.tooShort();

        KzFileParseResult result = parser.parseLines(List.of(valid20, invalid, valid21));

        assertEquals(2, result.lines().size());
        assertEquals(1, result.errors().size());
        assertEquals(2, result.errors().get(0).lineNumber());
        assertEquals("11111111", result.lines().get(0).wagonCode());
        assertEquals("22222222", result.lines().get(1).wagonCode());
        assertTrue(result.lines().get(1).containers().isPresent());
    }

    @Test
    void preservesOrderOfValidLines() {
        KzFileParseResult result = parser.parseContent(
                SyntheticKzLines.line20("1", "S", "1")
                        + "\n"
                        + SyntheticKzLines.line20("2", "S", "1")
                        + "\n"
                        + SyntheticKzLines.line20("3", "S", "1"));

        assertEquals(List.of("1", "2", "3"), result.lines().stream().map(KzDislocationLine::wagonCode).toList());
    }
}
