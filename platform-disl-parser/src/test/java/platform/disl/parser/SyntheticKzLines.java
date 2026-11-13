package platform.disl.parser;

/** Synthetic tab-separated KZ lines for unit tests. Not claimed to match production files. */
public final class SyntheticKzLines {

    private SyntheticKzLines() {
    }

    public static String line20(
            String wagonCode,
            String stationCode,
            String operationCode) {
        return String.join(
                "\t",
                wagonCode,
                stationCode,
                "idx",
                "2026-01-01 10:00:00",
                operationCode,
                "dest",
                "dep",
                "recv",
                "send",
                "type",
                "нет информации",
                "nod",
                "1",
                "1",
                " doc ",
                "cargo",
                "10",
                "70",
                "msg",
                "2026-01-01 09:00:00");
    }

    public static String line21(
            String wagonCode,
            String stationCode,
            String operationCode,
            String containers) {
        return line20(wagonCode, stationCode, operationCode) + "\t" + containers;
    }

    public static String tooShort() {
        return "only\tfew\tfields";
    }
}
