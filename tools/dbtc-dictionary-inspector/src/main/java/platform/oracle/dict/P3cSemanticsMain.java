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
 * P3C-1 semantics probes. Credentials from env only.
 * Runs G0 gate then G31/RP1/WH1/KIT_FK. Skips NK1/DT1/EXPLAIN PLAN.
 */
public final class P3cSemanticsMain {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final int QUERY_TIMEOUT_SEC = 30;
    private static final int MAX_ROWS = 5_000;

    private static final String[] SCRIPT_ORDER = {
        "G31_watched_dictionary.sql",
        "RP1_open_repair_probes.sql",
        "WH1_warehouse_semantics_probes.sql",
        "KIT_FK_import_link_probes.sql"
    };

    public static void main(String[] args) throws Exception {
        Path repoRoot = resolveRepoRoot(Path.of("").toAbsolutePath().normalize());
        InspectorConfig cfg = InspectorConfig.fromEnv(System.getenv(), repoRoot);
        Path sqlDir = repoRoot.resolve("docs/architecture/sql/dba/p3c/run");
        Path outDir = repoRoot.resolve("docs/architecture/sql/dba/p3c_results");

        System.out.println("=== P3C-1 DBTC semantics inspector ===");
        System.out.println("Started: " + Instant.now());
        System.out.println("URL (redacted): " + cfg.redactedUrl());
        System.out.println("User: " + (isBlank(cfg.user()) ? "<not set>" : cfg.user()));
        System.out.println("Password: " + (isBlank(cfg.password()) ? "<not set>" : "<set, not printed>"));
        System.out.println("SQL dir: " + sqlDir);
        System.out.println("Out dir: " + outDir);
        System.out.println("SKIP: NK1, DT1, EXPLAIN PLAN (no approved DBA access plan / PLAN_TABLE write)");

        if (!cfg.ok()) {
            for (String p : cfg.problems()) {
                System.out.println("FAIL: " + p);
            }
            System.err.println("STOP: credentials unavailable. No Oracle connection.");
            System.exit(1);
            return;
        }
        if (!Files.isDirectory(sqlDir)) {
            System.err.println("STOP: SQL directory missing: " + sqlDir);
            System.exit(2);
            return;
        }

        Files.createDirectories(outDir);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        Path manifest = outDir.resolve("run_manifest_" + stamp + ".csv");

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

            GateResult gate = runGate(conn, outDir, stamp, runs);
            if (!gate.ok()) {
                abort = true;
                abortReason = gate.reason();
                System.err.println("STOP after G0/gate: " + abortReason);
            } else {
                System.out.println("G0 gate OK: DB_NAME=" + gate.dbName()
                        + " SERVICE_NAME=" + gate.serviceName()
                        + " SESSION_USER=" + gate.sessionUser());

                for (String script : SCRIPT_ORDER) {
                    Path file = sqlDir.resolve(script);
                    if (!Files.isRegularFile(file)) {
                        runs.add(RunRow.fail(script, "-", "MISSING_FILE", 0, 0, "file not found"));
                        abort = true;
                        abortReason = "Missing SQL script " + script;
                        break;
                    }
                    List<SqlScriptParser.Statement> stmts = SqlScriptParser.parseFile(file);
                    System.out.println("--- " + script + " (" + stmts.size() + " statements) ---");
                    for (SqlScriptParser.Statement st : stmts) {
                        RunRow row = executeSql(conn, st.id(), st.sql(),
                                outDir.resolve(st.id() + "_" + stamp + ".csv"));
                        runs.add(row);
                        System.out.println(row.summary());
                        if ("ERROR".equals(row.status()) && isHardStop(row.error())) {
                            abort = true;
                            abortReason = "Hard stop on " + st.id() + ": " + row.error();
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

        runs.add(RunRow.fail("NK1", "-", "BLOCKED", 0, 0,
                "No approved DBA access plan for STATEREPORTRECORDS; EXPLAIN PLAN not run"));
        runs.add(RunRow.fail("DT1", "-", "BLOCKED", 0, 0,
                "Tape hour sample deferred until DBA confirms IDX_REC_GETTING_DATE plan"));

        writeManifest(manifest, runs);
        System.out.println("--- Manifest ---");
        System.out.println(manifest);
        System.out.println("Statements recorded: " + runs.size());
        if (abort) {
            System.err.println("P3C-1 incomplete: " + abortReason);
            System.exit(4);
            return;
        }
        System.out.println("P3C-1 finished. Disconnect complete. Analyze CSV offline only. Do not start P4.");
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
                service = rs.getString("service_name");
                host = rs.getString("server_host");
            }
        }

        System.out.println("SESSION_USER=" + sessionUser);
        System.out.println("CURRENT_SCHEMA=" + currentSchema);
        System.out.println("DB_NAME=" + dbName);
        System.out.println("SERVICE_NAME=" + service);
        System.out.println("SERVER_HOST=" + host);

        if (sessionUser != null) {
            String u = sessionUser.toUpperCase(Locale.ROOT);
            if (u.equals("RAIL_APP") || u.equals("DBTC") || u.equals("SYS") || u.equals("SYSTEM")) {
                return GateResult.fail("Forbidden SESSION_USER for P3C: " + sessionUser);
            }
        }
        if (!InspectorConfig.EXPECTED_DB_NAME.equalsIgnoreCase(nullToEmpty(dbName))) {
            return GateResult.fail("DB_NAME mismatch: expected RAC, got " + dbName);
        }
        if (!InspectorConfig.EXPECTED_SERVICE.equalsIgnoreCase(nullToEmpty(service))) {
            return GateResult.fail("SERVICE_NAME mismatch: expected RAC.WORLD, got " + service);
        }

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

        String neededSql = """
                SELECT table_name
                FROM all_tables
                WHERE owner = 'DBTC'
                  AND table_name IN (
                        'WAGON', 'REPAIRS', 'STATEREPORTS', 'IMPORTFILES',
                        'WAREHOUSEDETAILS', 'WAREHOUSES',
                        'WAGONSTATEREPORTS', 'WAGONSTATEREPORTSRECORDS'
                  )
                ORDER BY 1
                """;
        Path needCsv = outDir.resolve("G0_needed_objects_" + stamp + ".csv");
        RunRow needRun = executeSql(conn, "G0_needed_objects", neededSql, needCsv);
        runs.add(needRun);
        System.out.println(needRun.summary());
        if (needRun.rowCount() < 6) {
            return GateResult.fail("Insufficient object visibility for P3C-1 safe probes: "
                    + needRun.rowCount() + " tables visible");
        }

        return GateResult.ok(dbName, service, sessionUser);
    }

    private static RunRow executeSql(Connection conn, String id, String sql, Path csv) {
        long t0 = System.nanoTime();
        try (Statement statement = conn.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SEC);
            statement.setMaxRows(MAX_ROWS + 1);
            statement.setFetchSize(500);
            try (ResultSet rs = statement.executeQuery(sql)) {
                int rows = CsvResultWriter.write(csv, rs);
                long ms = (System.nanoTime() - t0) / 1_000_000L;
                boolean truncated = rows > MAX_ROWS;
                if (truncated) {
                    return RunRow.truncated(id, csv.getFileName().toString(), MAX_ROWS, ms);
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
            if (Files.isDirectory(p.resolve("docs/architecture/sql/dba/p3c"))) {
                return p;
            }
            Path parent = p.getParent();
            if (parent == null) {
                break;
            }
            p = parent;
        }
        Path fromTool = start.resolve("../..").normalize();
        if (Files.isDirectory(fromTool.resolve("docs/architecture/sql/dba/p3c"))) {
            return fromTool;
        }
        return start;
    }

    private static boolean isHardStop(String msg) {
        if (msg == null) {
            return false;
        }
        String m = msg.toUpperCase(Locale.ROOT);
        return m.contains("ORA-01031")
                || m.contains("ORA-01013")
                || m.contains("TIMEOUT")
                || m.contains("READ TIMED OUT")
                || m.contains("INSUFFICIENT PRIV");
    }

    private static String sanitize(String msg) {
        if (msg == null) {
            return "";
        }
        return msg.replaceAll("(?i)password\\s*=\\s*\\S+", "password=***")
                .replace('\n', ' ')
                .replace('\r', ' ');
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
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
