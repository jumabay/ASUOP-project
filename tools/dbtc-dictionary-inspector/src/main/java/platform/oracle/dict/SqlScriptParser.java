package platform.oracle.dict;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Splits audited .sql worksheets into individual SELECT statements. */
public final class SqlScriptParser {

    public record Statement(String id, String sql) {}

    public static List<Statement> parseFile(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        String withoutBlockComments = text.replaceAll("(?s)/\\*.*?\\*/", "\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : withoutBlockComments.split("\n", -1)) {
            String t = line.trim();
            if (t.startsWith("--")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        String[] parts = cleaned.toString().split(";");
        List<Statement> out = new ArrayList<>();
        int i = 1;
        String stem = file.getFileName().toString().replace(".sql", "");
        for (String part : parts) {
            String sql = part.trim();
            if (sql.isEmpty()) {
                continue;
            }
            String upper = sql.toUpperCase(Locale.ROOT);
            if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
                throw new IllegalStateException("Non-SELECT statement in " + file + ": " + preview(sql));
            }
            if (upper.contains(" COUNT(*)") && upper.contains("STATEREPORTRECORDS")
                    && !upper.contains("ALL_")) {
                throw new IllegalStateException("Forbidden COUNT on business table in " + file);
            }
            out.add(new Statement(stem + "_" + i, sql));
            i++;
        }
        return out;
    }

    private static String preview(String sql) {
        String one = sql.replaceAll("\\s+", " ");
        return one.length() > 120 ? one.substring(0, 120) + "…" : one;
    }
}
