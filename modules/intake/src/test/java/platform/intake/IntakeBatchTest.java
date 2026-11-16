package platform.intake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntakeBatchTest {

    @Test
    void copiesEventList() {
        NormalizedDislocationEvent event = new NormalizedDislocationEvent(
                "1",
                "180006",
                "S",
                Instant.parse("2025-11-15T10:15:00Z"),
                "08",
                "08",
                "ARR",
                "t",
                "",
                "",
                "",
                "y",
                "",
                "",
                "",
                "k",
                "h",
                "RWL",
                Instant.parse("2025-11-15T10:16:00Z"));
        IntakeBatch batch = new IntakeBatch("key", "RWL", "sum", List.of(event), List.of());
        assertEquals(1, batch.events().size());
        assertEquals("1", batch.events().get(0).wagonCode());
    }
}
