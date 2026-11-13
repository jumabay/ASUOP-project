package platform.disl.parser;

/** Shared synthetic file bodies for journal tests. Not production samples. */
public final class SyntheticSupport {

    private SyntheticSupport() {
    }

    public static String twoSameStationLines() {
        return SyntheticKzLines.line20("11111111", "700001", "3")
                + "\n"
                + SyntheticKzLines.line20("22222222", "700001", "3");
    }
}
