package platform.oracle.synthetic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Reproducible SYN-* wagon code generator for offline scale fixtures (P4-1 A).
 *
 * <p>Same {@code (count, seed)} always yields the same ordered list. Codes are unique,
 * zero-padded {@code SYN-#####} values drawn from {@code 1..count} then shuffled by seed.
 * This is <strong>not</strong> an Oracle load proof.
 */
public final class SyntheticWagonCodeGenerator {

    public static final long DEFAULT_SEED = 2026_11_20L;

    /** Default classpath fixture size (T0 / normal CI). */
    public static final int SIZE_T0 = 250;

    /** Soft scale tier (~18k registry). */
    public static final int SIZE_18K = 18_000;

    /** T1 minimum registry size from scale strategy (~18 334). */
    public static final int SIZE_T1 = 18_334;

    /** Upper soft scale tier. */
    public static final int SIZE_30K = 30_000;

    private SyntheticWagonCodeGenerator() {
    }

    /**
     * Generates {@code count} unique {@code SYN-#####} codes.
     *
     * @param count number of wagons (must be &gt;= 1)
     * @param seed  shuffle seed for reproducibility
     */
    public static List<String> generate(int count, long seed) {
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1, got " + count);
        }
        ArrayList<Integer> ids = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            ids.add(i);
        }
        Collections.shuffle(ids, new Random(seed));

        LinkedHashSet<String> unique = new LinkedHashSet<>(count);
        for (int id : ids) {
            unique.add(formatCode(id));
        }
        if (unique.size() != count) {
            throw new IllegalStateException("Generator produced duplicates for count=" + count);
        }
        return List.copyOf(unique);
    }

    public static List<String> generate(int count) {
        return generate(count, DEFAULT_SEED);
    }

    public static String formatCode(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("id must be >= 1, got " + id);
        }
        return "SYN-%05d".formatted(id);
    }

    public static boolean isSyntheticCode(String code) {
        return code != null && code.startsWith("SYN-");
    }

    public static void requireSyntheticOnly(List<String> codes) {
        Objects.requireNonNull(codes, "codes");
        for (String code : codes) {
            if (!isSyntheticCode(code)) {
                throw new IllegalArgumentException("Non-SYN code in synthetic catalog: " + code);
            }
        }
    }
}
