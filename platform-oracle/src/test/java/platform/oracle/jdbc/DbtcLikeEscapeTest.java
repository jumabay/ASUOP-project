package platform.oracle.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DbtcLikeEscapeTest {

    @Test
    void nullStaysNull() {
        assertNull(DbtcLikeEscape.escapeLikeLiteral(null));
    }

    @Test
    void escapesWildcardsAndBackslash() {
        assertEquals("100\\%", DbtcLikeEscape.escapeLikeLiteral("100%"));
        assertEquals("a\\_b", DbtcLikeEscape.escapeLikeLiteral("a_b"));
        assertEquals("back\\\\slash", DbtcLikeEscape.escapeLikeLiteral("back\\slash"));
    }

    @Test
    void plainCodeUnchanged() {
        assertEquals("54787361", DbtcLikeEscape.escapeLikeLiteral("54787361"));
    }
}