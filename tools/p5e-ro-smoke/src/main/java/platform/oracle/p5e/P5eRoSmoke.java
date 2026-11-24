package platform.oracle.p5e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Time-boxed P5-E RO smoke (APPROVED prep §7). Credentials from env only.
 * Never logs password. SELECT-only. No EXECUTE / EXPLAIN / DML.
 */
public final class P5eRoSmoke {

    private static final String URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    private static final String EXPECTED_USER = "DBTC"; // Oracle normalizes Dbtc
    private static final String EXPECTED_SCHEMA = "DBTC";
    private static final String EXPECTED_SERVICE = "RAC.WORLD";
    private static final String EXPECTED_HOST_MARKER = "tc-asuvp-03";
    private static final String CONTROL_CODE = "54787361";
    private static final LocalDate CONTROL_DAY = LocalDate.of(2026, 11, 24);
    private static final int QUERY_TIMEOUT_SEC = 30;
    private static final int FETCH_LIMIT = 501;

    public static void main(String[] args) {
        String password = firstEnv("ASUOP_ORACLE_PASSWORD");
        String user = firstNonBlank(firstEnv("ASUOP_ORACLE_USER"), "Dbtc");

        System.out.println("=== P5-E RO smoke (SELECT-only) ===");
        System.out.println("URL(redacted)=" + URL);
        System.out.println("user_arg=" + user);
        System.out.println("password_env_set=" + (password != null && !password.isBlank()));
        System.out.println("control=" + CONTROL_CODE + " day=" + CONTROL_DAY);

        if (password == null || password.isBlank()) {
            System.out.println("SECTION=identity");
            System.out.println("STOP-ID: ASUOP_ORACLE_PASSWORD is unset (no connect)");
            System.exit(3);
            return;
        }

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "15000");
        props.setProperty("oracle.jdbc.ReadTimeout", "30000");

        try (Connection conn = DriverManager.getConnection(URL, props)) {
            conn.setReadOnly(true);
            conn.setAutoCommit(false);

            Identity id = readIdentity(conn);
            System.out.println("SECTION=identity");
            System.out.println("session_user=" + id.sessionUser);
            System.out.println("current_schema=" + id.currentSchema);
            System.out.println("db_name=" + id.dbName);
            System.out.println("service_name=" + id.serviceName);
            System.out.println("server_host=" + id.serverHost);

            String stop = identityStop(id);
            if (stop != null) {
                System.out.println(stop);
                System.exit(4);
                return;
            }
            System.out.println("identity=PASS");

            System.out.println("SECTION=metadata");
            metaColumns(conn, "WAGON");
            metaColumns(conn, "STATEREPORTRECORDS");
            boolean hasCode = columnExists(conn, "WAGON", "CODE");
            boolean hasId = columnExists(conn, "WAGON", "ID");
            boolean hasGetting = columnExists(conn, "STATEREPORTRECORDS", "GETTING_DATE");
            boolean hasWagonId = columnExists(conn, "STATEREPORTRECORDS", "WAGON_ID");
            boolean hasReportId = columnExists(conn, "STATEREPORTRECORDS", "REPORT_ID");
            System.out.println("unlock wagon.CODE=" + hasCode);
            System.out.println("unlock wagon.ID=" + hasId);
            System.out.println("unlock srr.GETTING_DATE=" + hasGetting);
            System.out.println("unlock srr.WAGON_ID=" + hasWagonId);
            System.out.println("unlock srr.REPORT_ID=" + hasReportId);
            listPassportSkips();

            if (!hasCode || !hasId) {
                System.out.println("STOP-SCOPE: WAGON.CODE/ID not visible in metadata");
                System.exit(5);
                return;
            }
            if (!hasGetting || !hasWagonId) {
                System.out.println("STOP-SCOPE: STATEREPORTRECORDS GETTING_DATE/WAGON_ID not visible");
                System.exit(5);
                return;
            }

            System.out.println("SECTION=business");
            Long wagonId = resolveWagon(conn);
            if (wagonId == null) {
                System.out.println("A2=NO_ROW wagon_code=" + CONTROL_CODE);
            } else {
                System.out.println("A2=PASS wagon_code=" + CONTROL_CODE + " wagon_id=" + wagonId);
                System.out.println("A2_field wagonCode=MAPPED:WAGON.CODE");
                System.out.println("A2_field model=SKIPPED_UNKNOWN_MAPPING");
                System.out.println("A2_field wagonKind=SKIPPED_UNKNOWN_MAPPING");
                System.out.println("A2_field ownerName=SKIPPED_UNKNOWN_MAPPING");
                System.out.println("A2_field homeStationCode=SKIPPED_UNKNOWN_MAPPING");
                System.out.println("A2_field homeStationName=SKIPPED_UNKNOWN_MAPPING");
                System.out.println("A2_field inventoryPark=SKIPPED_UNKNOWN_MAPPING");
                System.out.println("A2_field currentState=OUT_OF_SCOPE");

                int n = readDay(conn, wagonId, hasReportId);
                System.out.println("A1_rows_fetched=" + n);
                if (n >= FETCH_LIMIT) {
                    System.out.println("A1=OVERFLOW_DETECTED (>=501) → would map to HTTP 422");
                } else {
                    System.out.println("A1=PASS row_count=" + n + " (keys: reportId"
                            + (hasReportId ? "=MAPPED" : "=SKIPPED_UNKNOWN_MAPPING")
                            + ", wagonCode=MAPPED, dislocationTime=MAPPED:GETTING_DATE,"
                            + " stationCode=SKIPPED_UNKNOWN_MAPPING, operationCode=SKIPPED_UNKNOWN_MAPPING)");
                }
            }
            System.out.println("DONE=SMOKE_COMPLETE");
        } catch (SQLException e) {
            System.out.println("SECTION=error");
            System.out.println("SQLState=" + e.getSQLState());
            System.out.println("ErrorCode=" + e.getErrorCode());
            System.out.println("Message=" + sanitize(e.getMessage()));
            System.exit(1);
        }
    }

    private static void listPassportSkips() {
        System.out.println("note=passport joins not unlocked without exact FK names; no invented joins");
    }

    private static Identity readIdentity(Connection conn) throws SQLException {
        String sql = """
                SELECT USER AS session_user,
                       SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS current_schema,
                       SYS_CONTEXT('USERENV','DB_NAME') AS db_name,
                       SYS_CONTEXT('USERENV','SERVICE_NAME') AS service_name,
                       SYS_CONTEXT('USERENV','SERVER_HOST') AS server_host
                FROM DUAL
                """;
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(sql)) {
                if (!rs.next()) {
                    throw new SQLException("DUAL returned no row");
                }
                return new Identity(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5));
            }
        }
    }

    private static String identityStop(Identity id) {
        if (id.sessionUser == null || !EXPECTED_USER.equalsIgnoreCase(id.sessionUser.trim())) {
            return "STOP-ID: session_user mismatch expected=" + EXPECTED_USER + " got=" + id.sessionUser;
        }
        if (id.currentSchema == null || !EXPECTED_SCHEMA.equalsIgnoreCase(id.currentSchema.trim())) {
            return "STOP-ID: current_schema mismatch expected=" + EXPECTED_SCHEMA + " got=" + id.currentSchema;
        }
        if (id.serviceName == null || !EXPECTED_SERVICE.equalsIgnoreCase(id.serviceName.trim())) {
            return "STOP-ID: service_name mismatch expected=" + EXPECTED_SERVICE + " got=" + id.serviceName;
        }
        if (id.serverHost != null && !id.serverHost.toLowerCase(Locale.ROOT).contains("tc-asuvp")) {
            // host context may be short name; require marker if present
            return "STOP-ID: server_host unexpected got=" + id.serverHost;
        }
        if (!URL.toLowerCase(Locale.ROOT).contains(EXPECTED_HOST_MARKER)) {
            return "STOP-ID: URL host marker missing";
        }
        return null;
    }

    private static void metaColumns(Connection conn, String table) throws SQLException {
        String sql = """
                SELECT column_name, data_type
                FROM all_tab_columns
                WHERE owner = 'DBTC' AND table_name = ?
                ORDER BY column_id
                """;
        List<String> cols = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SEC);
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cols.add(rs.getString(1) + ":" + rs.getString(2));
                }
            }
        }
        System.out.println("table=DBTC." + table + " column_count=" + cols.size());
        // print only allow-list relevant names, not full dump of 75 cols into chat if huge — still need evidence
        Set<String> interesting = interestingColumns(table);
        for (String c : cols) {
            String name = c.substring(0, c.indexOf(':'));
            if (interesting.contains(name)) {
                System.out.println("col " + table + "." + c);
            }
        }
    }

    private static Set<String> interestingColumns(String table) {
        Set<String> s = new LinkedHashSet<>();
        if ("WAGON".equals(table)) {
            s.add("ID");
            s.add("CODE");
            s.add("LOCATIONSTATION_ID");
            s.add("OPERATION_ID");
            s.add("LOCATIONDATE");
            s.add("REPORTDATE");
            s.add("CURRENTSTATE_ID");
        } else {
            s.add("WAGON_ID");
            s.add("REPORT_ID");
            s.add("GETTING_DATE");
            s.add("LOCATION_DATE");
            s.add("OPERATION_ID");
            s.add("TRAIN_CODE");
            s.add("LOCATIONSTATION_ID");
            s.add("LOCATION_STATION_ID");
            s.add("LOCATIONSTATIONID");
        }
        return s;
    }

    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        String sql = """
                SELECT 1 FROM all_tab_columns
                WHERE owner = 'DBTC' AND table_name = ? AND column_name = ?
                FETCH FIRST 1 ROW ONLY
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SEC);
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Long resolveWagon(Connection conn) throws SQLException {
        String sql = """
                SELECT w.id AS wagon_id, w.code AS wagon_code
                FROM dbtc.wagon w
                WHERE w.code = ?
                FETCH FIRST 1 ROW ONLY
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SEC);
            ps.setString(1, CONTROL_CODE);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getLong(1);
            }
        }
    }

    private static int readDay(Connection conn, long wagonId, boolean hasReportId) throws SQLException {
        LocalDateTime from = CONTROL_DAY.atStartOfDay();
        LocalDateTime to = CONTROL_DAY.plusDays(1).atStartOfDay();
        String sql = hasReportId
                ? """
                    SELECT r.report_id, r.getting_date
                    FROM dbtc.statereportrecords r
                    WHERE r.wagon_id = ?
                      AND r.getting_date >= ?
                      AND r.getting_date < ?
                    ORDER BY r.getting_date ASC
                    FETCH FIRST 501 ROWS ONLY
                    """
                : """
                    SELECT r.getting_date
                    FROM dbtc.statereportrecords r
                    WHERE r.wagon_id = ?
                      AND r.getting_date >= ?
                      AND r.getting_date < ?
                    ORDER BY r.getting_date ASC
                    FETCH FIRST 501 ROWS ONLY
                    """;
        int n = 0;
        Timestamp first = null;
        Timestamp last = null;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SEC);
            ps.setLong(1, wagonId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    n++;
                    Timestamp ts = hasReportId ? rs.getTimestamp(2) : rs.getTimestamp(1);
                    if (first == null) {
                        first = ts;
                    }
                    last = ts;
                    if (n <= 3 || n == FETCH_LIMIT) {
                        if (hasReportId) {
                            System.out.println("A1_sample n=" + n + " report_id=" + rs.getObject(1)
                                    + " getting_date=" + ts);
                        } else {
                            System.out.println("A1_sample n=" + n + " getting_date=" + ts);
                        }
                    }
                }
            }
        }
        System.out.println("A1_first_getting_date=" + first);
        System.out.println("A1_last_getting_date=" + last);
        return n;
    }

    private static String firstEnv(String name) {
        String v = System.getenv(name);
        return v == null ? null : v;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static String sanitize(String msg) {
        if (msg == null) {
            return "";
        }
        return msg.replaceAll("(?i)password\\s*=\\s*\\S+", "password=***");
    }

    private record Identity(
            String sessionUser,
            String currentSchema,
            String dbName,
            String serviceName,
            String serverHost) {}
}
