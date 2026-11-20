package platform.oracle.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class SyntheticWagonCodeGeneratorTest {

    @Test
    void generateIsReproducibleForSameSeed() {
        List<String> a = SyntheticWagonCodeGenerator.generate(100, 42L);
        List<String> b = SyntheticWagonCodeGenerator.generate(100, 42L);
        assertEquals(a, b);
        assertEquals(100, a.size());
        assertEquals(100, new HashSet<>(a).size());
        assertTrue(a.stream().allMatch(SyntheticWagonCodeGenerator::isSyntheticCode));
    }

    @Test
    void differentSeedsProduceDifferentOrder() {
        List<String> a = SyntheticWagonCodeGenerator.generate(50, 1L);
        List<String> b = SyntheticWagonCodeGenerator.generate(50, 2L);
        assertEquals(new HashSet<>(a), new HashSet<>(b));
        assertNotEquals(a, b);
    }

    @Test
    void formatCodeZeroPads() {
        assertEquals("SYN-00001", SyntheticWagonCodeGenerator.formatCode(1));
        assertEquals("SYN-18334", SyntheticWagonCodeGenerator.formatCode(18_334));
        assertEquals("SYN-30000", SyntheticWagonCodeGenerator.formatCode(30_000));
    }

    @Test
    void rejectNonPositiveCount() {
        assertThrows(IllegalArgumentException.class, () -> SyntheticWagonCodeGenerator.generate(0));
    }
}
