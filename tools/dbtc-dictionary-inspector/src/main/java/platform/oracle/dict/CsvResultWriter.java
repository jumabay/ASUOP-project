package platform.oracle.dict;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CsvResultWriter {

    private CsvResultWriter() {}

    public static int write(Path file, ResultSet rs) throws SQLException, IOException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        List<String> header = new ArrayList<>(cols);
        for (int c = 1; c <= cols; c++) {
            header.add(md.getColumnLabel(c));
        }
        Files.createDirectories(file.getParent());
        int rows = 0;
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(join(header));
            w.newLine();
            while (rs.next()) {
                List<String> line = new ArrayList<>(cols);
                for (int c = 1; c <= cols; c++) {
                    String v = rs.getString(c);
                    line.add(escape(v));
                }
                w.write(join(line));
                w.newLine();
                rows++;
            }
        }
        return rows;
    }

    private static String join(List<String> cells) {
        return String.join(",", cells);
    }

    private static String escape(String v) {
        if (v == null) {
            return "";
        }
        boolean need = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String s = v.replace("\"", "\"\"");
        return need ? "\"" + s + "\"" : s;
    }
}
