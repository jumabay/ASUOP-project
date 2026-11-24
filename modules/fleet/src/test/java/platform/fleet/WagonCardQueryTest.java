package platform.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class WagonCardQueryTest {

    @Test
    void returnsCardWhenPresent() {
        WagonCard card = sample("SYN-00001");
        WagonCardQuery query = new WagonCardQuery(code -> Optional.of(card));
        assertEquals(card, query.getByCode(" SYN-00001 "));
    }

    @Test
    void throwsWhenMissing() {
        WagonCardQuery query = new WagonCardQuery(code -> Optional.empty());
        WagonNotFoundException ex = assertThrows(WagonNotFoundException.class, () -> query.getByCode("SYN-MISSING"));
        assertEquals("SYN-MISSING", ex.wagonCode());
    }

    @Test
    void rejectsBlankCode() {
        WagonCardQuery query = new WagonCardQuery(code -> Optional.empty());
        assertThrows(InvalidWagonCodeException.class, () -> query.getByCode("  "));
    }

    private static WagonCard sample(String code) {
        return new WagonCard(code, "12-132", "КРЫТЫЙ", "SYN-OWNER", "6900", "SYN-STATION", true, null);
    }
}
