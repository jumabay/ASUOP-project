package platform.oracle.p5e;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * Gate 0: read-only ALL_SOURCE / ALL_ARGUMENTS for DBTC.WAGONPACKAGE only.
 * No EXECUTE. Password from env ASUOP_ORACLE_PASSWORD.
 */
public final class G31WagonPackageSourceProbe {

    private static final String URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    private static final String USER = "Dbtc";
    private static final String OWNER = "DBTC";
    private static final String PACKAGE = "WAGONPACKAGE";

    private G31WagonPackageSourceProbe() {}

    public static void main(String[] args) throws Exception {
        String password = System.getenv("ASUOP_ORACLE_PASSWORD");
        if (password == null || password.isBlank()) {
            System.out.println("STOP-ID: ASUOP_ORACLE_PASSWORD unset");
            System.exit(2);
        }

        Path outDir = Path.of("docs", "analysis", "_oracle_tmp", "g31_wagonpackage");
        // Prefer repo-relative when run from tools; fall back to absolute ASUOP root via env.
        String root = System.getenv("ASUOP_ROOT");
        if (root != null && !root.isBlank()) {
            outDir = Path.of(root, "docs", "analysis", "_oracle_tmp", "g31_wagonpackage");
        } else {
            Path cwd = Path.of("").toAbsolutePath();
            if (cwd.getFileName() != null && cwd.getFileName().toString().equals("p5e-ro-smoke")) {
                outDir = cwd.getParent().getParent().resolve("docs/analysis/_oracle_tmp/g31_wagonpackage");
            }
        }
        Files.createDirectories(outDir);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());

        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", password);
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "15000");
        props.setProperty("oracle.jdbc.ReadTimeout", "30000");

        try (Connection c = DriverManager.getConnection(URL, props)) {
            c.setReadOnly(true);
            System.out.println("G31 identity ok user=" + c.getMetaData().getUserName()
                    + " schema=" + c.getSchema());

            dumpObjects(c, outDir, stamp);
            int specLines = dumpSource(c, outDir, stamp, "PACKAGE", "SPEC");
            int bodyLines = dumpSource(c, outDir, stamp, "PACKAGE BODY", "BODY");
            dumpArguments(c, outDir, stamp);

            System.out.println("G31 SPEC_lines=" + specLines);
            System.out.println("G31 BODY_lines=" + bodyLines);
            if (specLines == 0 && bodyLines == 0) {
                System.out.println("STOP-ID: ALL_SOURCE empty for DBTC.WAGONPACKAGE (no grant or not visible)");
            } else if (bodyLines == 0) {
                System.out.println("G31 BODY_STATUS=MISSING_OR_INVISIBLE");
            } else {
                System.out.println("G31 BODY_STATUS=EXPORTED");
            }
            System.out.println("G31 out_dir=" + outDir.toAbsolutePath());
            System.out.println("G31_PROBE_COMPLETE");
        }
    }

    private static void dumpObjects(Connection c, Path outDir, String stamp) throws Exception {
        String sql = """
                SELECT object_name, object_type, status, timestamp
                FROM all_objects
                WHERE owner = ?
                  AND object_name = ?
                  AND object_type IN ('PACKAGE', 'PACKAGE BODY')
                ORDER BY object_type
                """;
        Path file = outDir.resolve("objects_" + stamp + ".csv");
        try (PreparedStatement ps = c.prepareStatement(sql);
                BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            ps.setString(1, OWNER);
            ps.setString(2, PACKAGE);
            w.write("object_name,object_type,status,timestamp\n");
            try (ResultSet rs = ps.executeQuery()) {
                int n = 0;
                while (rs.next()) {
                    n++;
                    w.write(csv(rs.getString(1)) + "," + csv(rs.getString(2)) + ","
                            + csv(rs.getString(3)) + "," + csv(rs.getString(4)) + "\n");
                    System.out.println("G31 object type=" + rs.getString(2) + " status=" + rs.getString(3));
                }
                System.out.println("G31 object_rows=" + n);
            }
        }
    }

    private static void dumpArguments(Connection c, Path outDir, String stamp) throws Exception {
        // Oracle 12.2 ALL_ARGUMENTS: subprogram name is OBJECT_NAME (not PROCEDURE_NAME).
        String sql = """
                SELECT package_name, object_name, overload, position, argument_name,
                       data_type, in_out, data_level
                FROM all_arguments
                WHERE owner = ?
                  AND package_name = ?
                  AND object_name IN ('GETACTIVEWAGONLIST', 'GETWAGONLIST')
                ORDER BY object_name, overload, sequence
                """;
        Path file = outDir.resolve("arguments_" + stamp + ".csv");
        try (PreparedStatement ps = c.prepareStatement(sql);
                BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            ps.setString(1, OWNER);
            ps.setString(2, PACKAGE);
            w.write("package_name,object_name,overload,position,argument_name,data_type,in_out,data_level\n");
            try (ResultSet rs = ps.executeQuery()) {
                int n = 0;
                while (rs.next()) {
                    n++;
                    w.write(csv(rs.getString(1)) + "," + csv(rs.getString(2)) + ","
                            + rs.getString(3) + "," + rs.getString(4) + ","
                            + csv(rs.getString(5)) + "," + csv(rs.getString(6)) + ","
                            + csv(rs.getString(7)) + "," + rs.getString(8) + "\n");
                }
                System.out.println("G31 argument_rows=" + n);
            }
        } catch (java.sql.SQLException ex) {
            System.out.println("G31 arguments_SKIPPED err=" + ex.getErrorCode() + " " + ex.getMessage());
        }
    }

    private static int dumpSource(Connection c, Path outDir, String stamp, String type, String label)
            throws Exception {
        String sql = """
                SELECT line, text
                FROM all_source
                WHERE owner = ?
                  AND name = ?
                  AND type = ?
                ORDER BY line
                """;
        Path file = outDir.resolve(label.toLowerCase(Locale.ROOT) + "_" + stamp + ".sql");
        int lines = 0;
        boolean wrappedHint = false;
        try (PreparedStatement ps = c.prepareStatement(sql);
                BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            ps.setString(1, OWNER);
            ps.setString(2, PACKAGE);
            ps.setString(3, type);
            w.write("-- ALL_SOURCE owner=" + OWNER + " name=" + PACKAGE + " type=" + type
                    + " exported=" + Timestamp.valueOf(LocalDateTime.now()) + "\n");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines++;
                    String text = rs.getString(2);
                    if (text != null) {
                        String upper = text.toUpperCase(Locale.ROOT);
                        if (upper.contains("WRAPPED") || upper.contains("EDITIONABLE")) {
                            // keep scanning; wrapped packages often show short opaque lines
                        }
                        if (upper.contains("WRAPPED")) {
                            wrappedHint = true;
                        }
                        w.write(text);
                        if (!text.endsWith("\n")) {
                            w.write("\n");
                        }
                    }
                }
            }
        }
        if (wrappedHint) {
            System.out.println("G31 " + label + "_WRAPPED_HINT=true");
        }
        return lines;
    }

    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
