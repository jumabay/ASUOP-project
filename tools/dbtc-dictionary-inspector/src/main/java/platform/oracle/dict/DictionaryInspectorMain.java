package platform.oracle.dict;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * P3B automated dictionary inspector. Credentials from env only. SELECT metadata only.
 */
public final class DictionaryInspectorMain {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 120_000;
    private static final int QUERY_TIMEOUT_SEC = 90;
    private static final int MAX_ROWS_PER_QUERY = 200_000;
    private static final String DRIVER_ARTIFACT = "ojdbc10:19.33.0.0";

    private static final String[] SCRIPT_ORDER = {
        "G0_session_and_visibility.sql",
        "G1_table_structure_keys.sql",
        "G2_indexes_partitions_stats.sql",
        "G3_packages_dependencies.sql",
        "G4_repairs_warehouse_meta.sql",
        "G5_watched_g31_clues.sql"
    };

    public static void main(String[] args) throws Exception {
        Path repoRoot = resolveRepoRoot(Path.of("").toAbsolutePath().normalize());
        InspectorConfig cfg = InspectorConfig.fromEnv(System.getenv(), repoRoot);

        System.out.println("=== P3B DBTC dictionary inspector ===");
        System.out.println("Started: " + Instant.now());
        System.out.println("Repo: " + repoRoot);
        System.out.println("Driver artifact: " + DRIVER_ARTIFACT);
        System.out.println("URL (redacted): " + cfg.redactedUrl());
        System.out.println("User: " + (cfg.user() == null || cfg.user().isBlank() ? "<not set>" : cfg.user()));
        System.out.println("Password: " + (cfg.password() == null || cfg.password().isBlank()
                ? "<not set>" : "<set, not printed>"));
        System.out.println("SQL dir: " + cfg.sqlDir());
        System.out.println("Out dir: " + cfg.outDir());

        if (!cfg.ok()) {
            System.out.println("--- Validation ---");
            for (String p : cfg.problems()) {
                System.out.println("FAIL: " + p);
            }
            System.err.println("STOP: approved read-only credentials unavailable or invalid. No Oracle connection.");
            System.err.println("Required env (example names):");
            System.err.println("  P3B_ORACLE_USER / ORACLE_USER  — DBA-approved dictionary RO (not RAIL_APP/DBTC/SYS)");
            System.err.println("  P3B_ORACLE_PASSWORD / ORACLE_PASSWORD");
            System.err.println("  optional P3B_JDBC_URL / ORACLE_JDBC_URL (default tc-asuvp-03/RAC.WORLD)");
            System.exit(1);
            return;
        }

        if (!Files.isDirectory(cfg.sqlDir())) {
            System.err.println("STOP: SQL directory missing: " + cfg.sqlDir());
            System.exit(2);
            return;
        }

        Files.createDirectories(cfg.outDir());
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        Path manifest = cfg.outDir().resolve("run_manifest_" + stamp + ".csv");

        Properties props = new Properties();
        props.setProperty("user", cfg.user());
        props.setProperty("password", cfg.password());
        props.setProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(CONNECT_TIMEOUT_MS));
        props.setProperty("oracle.jdbc.ReadTimeout", String.valueOf(READ_TIMEOUT_MS));

        List<RunRow> runs = new ArrayList<>();
        boolean abort = false;
        String abortReason = null;

        try (Connection conn = DriverManager.getConnection(cfg.jdbcUrl(), props)) {
            conn.setAutoCommit(false);
            conn.setReadOnly(true);
            System.out.println("Connected. readOnly=" + conn.isReadOnly());

            GateResult gate = runGate(conn, cfg.outDir(), stamp, runs);
            if (!gate.ok()) {
                abort = true;
                abortReason = gate.reason();
                System.err.println("STOP after G0/gate: " + abortReason);
            } else {
                System.out.println("G0 gate OK: DB_NAME=" + gate.dbName()
                        + " SERVICE_NAME=" + gate.serviceName()
                        + " SESSION_USER=" + gate.sessionUser());
                // Skip G0 file statements already covered by gate CSVs; run G1–G5 only.
                for (String script : SCRIPT_ORDER) {
                    if (script.startsWith("G0_")) {
                        continue;
                    }
                    Path file = cfg.sqlDir().resolve(script);
                    if (!Files.isRegularFile(file)) {
                        runs.add(RunRow.fail(script, "-", "MISSING_FILE", 0, 0, "file not found"));
                        abort = true;
                        abortReason = "Missing SQL script " + script;
                        break;
                    }
                    List<SqlScriptParser.Statement> stmts = SqlScriptParser.parseFile(file);
                    System.out.println("--- " + script + " (" + stmts.size() + " statements) ---");
                    for (SqlScriptParser.Statement st : stmts) {
                        RunRow row = executeOne(conn, st, cfg.outDir(), stamp);
                        runs.add(row);
                        System.out.println(row.summary());
                        if ("ERROR".equals(row.status()) && isPrivilegeError(row.error())) {
                            abort = true;
                            abortReason = "Privilege error on " + st.id() + ": " + row.error();
                            break;
                        }
                    }
                    if (abort) {
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            abort = true;
            abortReason = "CONNECT FAILED: SQLState=" + e.getSQLState()
                    + " code=" + e.getErrorCode()
                    + " msg=" + sanitize(e.getMessage());
            System.err.println(abortReason);
            runs.add(RunRow.fail("CONNECT", "-", "ERROR", 0, 0, abortReason));
        }

        writeManifest(manifest, runs);
        System.out.println("--- Manifest ---");
        System.out.println(manifest);
        System.out.println("Statements recorded: " + runs.size());
        if (abort) {
            System.err.println("P3B incomplete: " + abortReason);
            System.exit(4);
            return;
        }
        System.out.println("P3B dictionary run finished. Disconnect complete. Analyze CSV offline only.");
    }

    private static GateResult runGate(Connection conn, Path outDir, String stamp, List<RunRow> runs)
            throws SQLException, IOException {
        String identitySql = """
                SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') AS session_user,
                       USER AS user_name,
                       SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS current_schema,
                       SYS_CONTEXT('USERENV', 'DB_NAME') AS db_name,
                       SYS_CONTEXT('USERENV', 'CON_NAME') AS con_name,
                       SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service_name,
                       SYS_CONTEXT('USERENV', 'SERVER_HOST') AS server_host
                FROM dual
                """;
        Path idCsv = outDir.resolve("G0_1_identity_" + stamp + ".csv");
        RunRow idRun = executeSql(conn, "G0_1_identity", identitySql, idCsv);
        runs.add(idRun);
        System.out.println(idRun.summary());
        if (!"OK".equals(idRun.status()) && !"TRUNCATED".equals(idRun.status())) {
            return GateResult.fail("Identity query failed: " + idRun.error());
        }

        String dbName = null;
        String service = null;
        String sessionUser = null;
        String currentSchema = null;
        String conName = null;
        String host = null;
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(identitySql)) {
                if (!rs.next()) {
                    return GateResult.fail("Identity returned 0 rows");
                }
                sessionUser = rs.getString("session_user");
                currentSchema = rs.getString("current_schema");
                dbName = rs.getString("db_name");
                conName = rs.getString("con_name");
                service = rs.getString("service_name");
                host = rs.getString("server_host");
            }
        }

        System.out.println("SESSION_USER=" + sessionUser);
        System.out.println("CURRENT_SCHEMA=" + currentSchema);
        System.out.println("DB_NAME=" + dbName);
        System.out.println("CON_NAME=" + conName);
        System.out.println("SERVICE_NAME=" + service);
        System.out.println("SERVER_HOST=" + host);

        if (sessionUser != null) {
            String u = sessionUser.toUpperCase(Locale.ROOT);
            if (u.equals("RAIL_APP") || u.equals("DBTC") || u.equals("SYS") || u.equals("SYSTEM")) {
                return GateResult.fail("Forbidden SESSION_USER for P3B: " + sessionUser);
            }
        }
        if (!InspectorConfig.EXPECTED_DB_NAME.equalsIgnoreCase(nullToEmpty(dbName))) {
            return GateResult.fail("DB_NAME mismatch: expected RAC, got " + dbName);
        }
        if (!InspectorConfig.EXPECTED_SERVICE.equalsIgnoreCase(nullToEmpty(service))) {
            return GateResult.fail("SERVICE_NAME mismatch: expected RAC.WORLD, got " + service);
        }

        // Privileges
        List<String> sessionPrivs = queryStrings(conn,
                "SELECT privilege FROM session_privs WHERE ROWNUM <= 200 ORDER BY 1");
        Path privCsv = outDir.resolve("G0_privs_session_" + stamp + ".csv");
        writeSingleColumnCsv(privCsv, "PRIVILEGE", sessionPrivs);
        runs.add(RunRow.ok("G0_privs_session", privCsv.getFileName().toString(), sessionPrivs.size(), 0, false));

        List<String> roles = queryStrings(conn,
                "SELECT role FROM session_roles WHERE ROWNUM <= 200 ORDER BY 1");
        Path roleCsv = outDir.resolve("G0_privs_roles_" + stamp + ".csv");
        writeSingleColumnCsv(roleCsv, "ROLE", roles);
        runs.add(RunRow.ok("G0_privs_roles", roleCsv.getFileName().toString(), roles.size(), 0, false));

        List<String> tabKinds = queryStrings(conn,
                "SELECT privilege || '=' || COUNT(*) FROM user_tab_privs "
                        + "GROUP BY privilege ORDER BY 1");
        Path tabCsv = outDir.resolve("G0_privs_tabkinds_" + stamp + ".csv");
        writeSingleColumnCsv(tabCsv, "PRIVILEGE_KIND", tabKinds);
        runs.add(RunRow.ok("G0_privs_tabkinds", tabCsv.getFileName().toString(), tabKinds.size(), 0, false));

        PrivilegeClassifier.Assessment assessment =
                PrivilegeClassifier.assess(sessionPrivs, roles, tabKinds);
        System.out.println("Privileges allowed: " + assessment.allowedRead());
        System.out.println("Privileges notes: " + assessment.broadReadNotes());
        System.out.println("Privileges dangerous: " + assessment.writeOrAdmin());
        if (!assessment.isSafeToContinue()) {
            return GateResult.fail("Write/admin privileges: " + assessment.writeOrAdmin());
        }

        String visSql = """
                SELECT 'ALL_TABLES' AS probe,
                       COUNT(*) AS visible_rows
                FROM all_tables
                WHERE owner = 'DBTC'
                  AND table_name IN ('WAGON', 'STATEREPORTRECORDS', 'IMPORTFILES')
                UNION ALL
                SELECT 'ALL_OBJECTS_WAGON',
                       COUNT(*)
                FROM all_objects
                WHERE owner = 'DBTC'
                  AND object_name = 'WAGON'
                  AND object_type = 'TABLE'
                """;
        Path visCsv = outDir.resolve("G0_2_visibility_" + stamp + ".csv");
        RunRow visRun = executeSql(conn, "G0_2_visibility", visSql, visCsv);
        runs.add(visRun);
        System.out.println(visRun.summary());
        if (!"OK".equals(visRun.status()) && !"TRUNCATED".equals(visRun.status())) {
            return GateResult.fail("Visibility query failed: " + visRun.error());
        }

        long visible = 0;
        long wagonVisible = 0;
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(visSql)) {
                while (rs.next()) {
                    String probe = rs.getString("probe");
                    long n = rs.getLong("visible_rows");
                    visible += n;
                    if ("ALL_OBJECTS_WAGON".equalsIgnoreCase(probe)) {
                        wagonVisible = n;
                    }
                }
            }
        }
        if (visible <= 0 || wagonVisible <= 0) {
            return GateResult.fail("DBTC dictionary visibility = 0 (ALL_*). Need addressed SELECT grants; do not escalate privileges.");
        }

        // Core tape/import must be visible for a complete P3B; otherwise mark PARTIAL and stop before G1 noise.
        String coreVisSql = """
                SELECT table_name
                FROM all_tables
                WHERE owner = 'DBTC'
                  AND table_name IN ('STATEREPORTRECORDS', 'IMPORTFILES', 'STATEREPORTS')
                ORDER BY 1
                """;
        Path coreCsv = outDir.resolve("G0_3_core_visibility_" + stamp + ".csv");
        RunRow coreRun = executeSql(conn, "G0_3_core_visibility", coreVisSql, coreCsv);
        runs.add(coreRun);
        System.out.println(coreRun.summary());
        if (coreRun.rowCount() < 3) {
            System.out.println("WARNING: core tape/import tables not fully visible ("
                    + coreRun.rowCount() + "/3). Continue G1+ only for granted objects; P3B remains PARTIAL.");
        }

        return GateResult.ok(dbName, service, sessionUser);
    }

    private static RunRow executeOne(Connection conn, SqlScriptParser.Statement st, Path outDir, String stamp)
            throws SQLException {
        Path csv = outDir.resolve(st.id() + "_" + stamp + ".csv");
        return executeSql(conn, st.id(), st.sql(), csv);
    }

    private static RunRow executeSql(Connection conn, String id, String sql, Path csv) {
        long t0 = System.nanoTime();
        try (Statement statement = conn.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SEC);
            statement.setMaxRows(MAX_ROWS_PER_QUERY + 1);
            try (ResultSet rs = statement.executeQuery(sql)) {
                int rows = CsvResultWriter.write(csv, rs);
                long ms = (System.nanoTime() - t0) / 1_000_000L;
                boolean truncated = rows > MAX_ROWS_PER_QUERY;
                if (truncated) {
                    // rewrite note: last row may be extra; mark incomplete
                    return RunRow.truncated(id, csv.getFileName().toString(), MAX_ROWS_PER_QUERY, ms);
                }
                return RunRow.ok(id, csv.getFileName().toString(), rows, ms, false);
            }
        } catch (Exception e) {
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            return RunRow.fail(id, csv.getFileName().toString(), "ERROR", 0, ms, sanitize(e.getMessage()));
        }
    }

    private static List<String> queryStrings(Connection conn, String sql) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }

    private static void writeSingleColumnCsv(Path file, String header, List<String> values) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(header);
            w.newLine();
            for (String v : values) {
                w.write(v == null ? "" : v.replace("\"", "\"\""));
                w.newLine();
            }
        }
    }

    private static void writeManifest(Path manifest, List<RunRow> runs) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(manifest, StandardCharsets.UTF_8)) {
            w.write("id,csv_file,status,row_count,duration_ms,incomplete,error");
            w.newLine();
            for (RunRow r : runs) {
                w.write(r.toCsvLine());
                w.newLine();
            }
        }
    }

    private static Path resolveRepoRoot(Path start) {
        Path p = start;
        for (int i = 0; i < 8; i++) {
            if (Files.isDirectory(p.resolve("docs/architecture/sql/dba/p3b"))) {
                return p;
            }
            Path parent = p.getParent();
            if (parent == null) {
                break;
            }
            p = parent;
        }
        // When run from tool dir
        Path fromTool = start.resolve("../..").normalize();
        if (Files.isDirectory(fromTool.resolve("docs/architecture/sql/dba/p3b"))) {
            return fromTool;
        }
        return start;
    }

    private static boolean isPrivilegeError(String msg) {
        if (msg == null) {
            return false;
        }
        String m = msg.toUpperCase(Locale.ROOT);
        return m.contains("ORA-00942") || m.contains("ORA-01031") || m.contains("INSUFFICIENT PRIV");
    }

    private static String sanitize(String msg) {
        if (msg == null) {
            return "";
        }
        return msg.replaceAll("(?i)password\\s*=\\s*\\S+", "password=***")
                .replace('\n', ' ')
                .replace('\r', ' ');
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private record GateResult(boolean ok, String reason, String dbName, String serviceName, String sessionUser) {
        static GateResult ok(String db, String svc, String user) {
            return new GateResult(true, null, db, svc, user);
        }

        static GateResult fail(String reason) {
            return new GateResult(false, reason, null, null, null);
        }
    }

    private record RunRow(
            String id, String csvFile, String status, int rowCount, long durationMs, boolean incomplete, String error) {

        static RunRow ok(String id, String csv, int rows, long ms, boolean incomplete) {
            return new RunRow(id, csv, incomplete ? "TRUNCATED" : "OK", rows, ms, incomplete, "");
        }

        static RunRow truncated(String id, String csv, int rows, long ms) {
            return new RunRow(id, csv, "TRUNCATED", rows, ms, true, "row limit reached; result incomplete");
        }

        static RunRow fail(String id, String csv, String status, int rows, long ms, String err) {
            return new RunRow(id, csv, status, rows, ms, false, err == null ? "" : err);
        }

        String summary() {
            return id + " status=" + status + " rows=" + rowCount + " ms=" + durationMs
                    + (incomplete ? " INCOMPLETE" : "")
                    + (error.isEmpty() ? "" : " err=" + error);
        }

        String toCsvLine() {
            return String.join(",",
                    esc(id),
                    esc(csvFile),
                    esc(status),
                    Integer.toString(rowCount),
                    Long.toString(durationMs),
                    Boolean.toString(incomplete),
                    esc(error));
        }

        private static String esc(String v) {
            if (v == null) {
                return "";
            }
            if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                return "\"" + v.replace("\"", "\"\"") + "\"";
            }
            return v;
        }
    }
}
