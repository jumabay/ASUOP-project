package platform.oracle.migration.l0;

import java.util.List;

/**
 * Gaps that L0 must NOT claim closed (doc 44 §5).
 */
public final class OpenGaps {

    public static final String NK1 = "NK1";
    public static final String DT1 = "DT1";
    public static final String G31 = "G31";
    public static final String R1_Q_TZ = "R1-Q-TZ";

    private OpenGaps() {
    }

    public static List<String> stillOpen() {
        return List.of(NK1, DT1, G31, R1_Q_TZ);
    }
}
