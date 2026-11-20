package platform.rwl.proto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import platform.rwl.proto.model.RunStatus;
import platform.rwl.proto.parse.ParseOutcome;
import platform.rwl.proto.parse.RwlXmlParser;

class RwlXmlParserTest {

    private final RwlXmlParser parser = new RwlXmlParser();
    private final Instant fetched = Instant.parse("2025-11-16T04:00:00Z");

    @Test
    void parsesDocumentedXmlFixture() throws Exception {
        String xml = Files.readString(fixture("success-two-wagons.xml"));
        ParseOutcome outcome = parser.parse(xml, fetched);
        assertEquals(RunStatus.SUCCESS, outcome.status());
        assertEquals(2, outcome.wagonsSeen());
        assertEquals(2, outcome.operations().size());
        assertEquals(LocalDateTime.of(2025, 11, 15, 10, 15, 0), outcome.operations().get(0).operationTimeUtc());
        assertEquals("12345678", outcome.operations().get(0).wagonNo());
        assertTrue(outcome.operations().get(0).eventKey().isPresent());
    }

    @Test
    void emptyDataIsEmptyStatus() throws Exception {
        String xml = Files.readString(fixture("empty-vagons.xml"));
        ParseOutcome outcome = parser.parse(xml, fetched);
        assertEquals(RunStatus.EMPTY, outcome.status());
        assertEquals(0, outcome.operations().size());
    }

    @Test
    void plainApiErrorDetected() throws Exception {
        String body = Files.readString(fixture("api-error-plain.txt"));
        ParseOutcome outcome = parser.parse(body, fetched);
        assertEquals(RunStatus.API_ERROR, outcome.status());
        assertTrue(outcome.errorSummary().contains("incorrect_user_login"));
    }

    @Test
    void missingUtcGoesToRejectNotOperation() throws Exception {
        String xml = Files.readString(fixture("missing-utc.xml"));
        ParseOutcome outcome = parser.parse(xml, fetched);
        assertEquals(RunStatus.SUCCESS, outcome.status());
        assertEquals(0, outcome.operations().size());
        assertEquals(1, outcome.rejects().size());
        assertEquals("missing_utc_datetime", outcome.rejects().get(0).reasonCode());
    }

    private static Path fixture(String name) {
        return Path.of("src/test/resources/fixtures").resolve(name);
    }
}
