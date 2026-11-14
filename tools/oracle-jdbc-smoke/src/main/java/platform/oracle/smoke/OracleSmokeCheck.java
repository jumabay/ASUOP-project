package platform.oracle.smoke;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * One-shot JDBC smoke check. Credentials from environment only.
 *
 * Default: --dry-run (no network).
 * Connect: --connect
 * Potential industrial host (tc-asuvp-03 / RAC.WORLD) additionally requires
 * --allow-potential-prod-smoke-once for this single run only (no permanent allow-list).
 */
public final class OracleSmokeCheck {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int QUERY_TIMEOUT_SEC = 15;
    private static final String EXPECTED_DRIVER_ARTIFACT = "ojdbc10:19.33.0.0";

    private static final String[] FORBIDDEN_HOST_MARKERS = {
            "tc-asuvp-03",
            "/rac.world"
    };

    public static void main(String[] args) {
        boolean connect = false;
        boolean allowPotentialProdOnce = false;

        for (String arg : args) {
            switch (arg) {
                case "--connect" -> connect = true;
                case "--dry-run" -> connect = false;
                case "--allow-potential-prod-smoke-once" -> allowPotentialProdOnce = true;
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                default -> {
                    System.err.println("Unknown argument: " + arg);
                    printHelp();
                    System.exit(2);
                    return;
                }
            }
        }

        String url = System.getenv("ORACLE_JDBC_URL");
        String user = System.getenv("ORACLE_USER");
        String password = System.getenv("ORACLE_PASSWORD");

        System.out.println("=== Oracle JDBC smoke (Java 21) ===");
        System.out.println("Mode: " + (connect ? "CONNECT" : "DRY-RUN (no network)"));
        System.out.println("Approved driver artifact: " + EXPECTED_DRIVER_ARTIFACT);
        System.out.println("Java: " + System.getProperty("java.version")
                + " (" + System.getProperty("java.home") + ")");
        System.out.println("Target risk: URL may point to POTENTIALLY INDUSTRIAL Oracle");

        List<String> problems = new ArrayList<>();
        if (isBlank(url)) {
            problems.add("ORACLE_JDBC_URL is missing");
        }
        if (isBlank(user)) {
            problems.add("ORACLE_USER is missing");
        } else if (!"RAILWAY_READONLY".equals(user)) {
            problems.add("ORACLE_USER must be RAILWAY_READONLY (got: " + user + ")");
        }
        if (isBlank(password)) {
            problems.add("ORACLE_PASSWORD is missing");
        }

        String blocked = isBlank(url) ? null : findForbiddenHost(url);
        if (blocked != null && !(connect && allowPotentialProdOnce)) {
            problems.add("ORACLE_JDBC_URL contains industrial marker '" + blocked
                    + "'. One-time connect requires --connect --allow-potential-prod-smoke-once");
        }
        if (blocked != null && connect && allowPotentialProdOnce) {
            System.out.println("NOTICE: one-time override for potential industrial host marker '"
                    + blocked + "' (not a permanent allow-list)");
        }

        if (!isBlank(url)) {
            System.out.println("URL (redacted): " + redactUrl(url));
        } else {
            System.out.println("URL: <not set>");
        }
        System.out.println("User: " + (isBlank(user) ? "<not set>" : user));
        System.out.println("Password: " + (isBlank(password) ? "<not set>" : "<set, not printed>"));

        if (!problems.isEmpty()) {
            System.out.println("--- Validation ---");
            for (String p : problems) {
                System.out.println("FAIL: " + p);
            }
            System.err.println("Stopping: environment/validation incomplete. No Oracle connection.");
            System.exit(1);
            return;
        }

        System.out.println("--- Validation ---");
        System.out.println("OK: env present, user RAILWAY_READONLY");
        assertNlsCharsetSupport();

        if (!connect) {
            System.out.println("Dry-run complete. No connection attempted.");
            return;
        }

        runConnect(url, user, password);
    }

    /**
     * CL8MSWIN1251 Oracle charset id = 171. Requires orai18n on the classpath
     * (fixes JDBC error 17056 without changing database NLS parameters).
     */
    private static void assertNlsCharsetSupport() {
        final int cl8mswin1251 = 171;
        try {
            Class<?> characterSet = Class.forName("oracle.sql.CharacterSet");
            Object cs = characterSet.getMethod("make", int.class).invoke(null, cl8mswin1251);
            if (cs == null) {
                System.err.println("STOP: oracle.sql.CharacterSet.make(171) returned null (orai18n missing?)");
                System.exit(6);
                return;
            }
            System.out.println("NLS: CL8MSWIN1251 (id 171) loadable via orai18n: OK (" + cs + ")");
        } catch (ClassNotFoundException e) {
            System.err.println("STOP: oracle.sql.CharacterSet not found — add com.oracle.database.nls:orai18n:19.33.0.0");
            System.exit(6);
        } catch (ReflectiveOperationException e) {
            System.err.println("STOP: cannot load CL8MSWIN1251 via orai18n: " + e);
            System.exit(6);
        }
    }

    private static void runConnect(String url, String user, String password) {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(CONNECT_TIMEOUT_MS));
        props.setProperty("oracle.jdbc.ReadTimeout", String.valueOf(READ_TIMEOUT_MS));

        System.out.println("--- Connect (read-only session intent) ---");
        try (Connection conn = DriverManager.getConnection(url, props)) {
            conn.setAutoCommit(false);
            conn.setReadOnly(true);

            String driverName = conn.getMetaData().getDriverName();
            String driverVersion = conn.getMetaData().getDriverVersion();
            System.out.println("JDBC driver: " + driverName + " " + driverVersion);
            assertApprovedDriver(driverName, driverVersion);

            // Privileges first — stop before further diagnostics if write/admin.
            sessionIdentity(conn);
            if (!privilegeCheck(conn)) {
                System.err.println("STOP: write/admin privileges detected. No further queries.");
                System.exit(4);
                return;
            }

            dualCheck(conn);
            versionCheck(conn);
            dbtcSchemaPresenceOnly(conn);

            System.out.println("--- Result ---");
            System.out.println("Smoke CONNECT completed within approved diagnostic scope.");
        } catch (SQLException e) {
            System.err.println("CONNECT FAILED: SQLState=" + e.getSQLState()
                    + " errorCode=" + e.getErrorCode()
                    + " message=" + sanitizeMessage(e.getMessage()));
            System.err.println("Compatibility/driver change requires separate approval. Not changing driver.");
            System.exit(3);
        }
    }

    private static void assertApprovedDriver(String driverName, String driverVersion) {
        String combined = (driverName + " " + driverVersion).toLowerCase(Locale.ROOT);
        if (combined.contains("23.") || combined.contains("ojdbc11")) {
            System.err.println("STOP: forbidden driver on connection: " + driverName + " " + driverVersion);
            System.exit(5);
        }
        // Oracle often reports "Oracle JDBC driver" + "19.x.x.x.x" — require 19. and not 23.
        if (!combined.contains("19.")) {
            System.err.println("STOP: driver version does not look like 19.x: " + driverVersion);
            System.exit(5);
        }
        System.out.println("Driver gate: accepted as 19.x line (artifact pinned to " + EXPECTED_DRIVER_ARTIFACT + ")");
    }

    private static void sessionIdentity(Connection conn) throws SQLException {
        String sql = """
                SELECT
                  SYS_CONTEXT('USERENV','SESSION_USER') AS session_user,
                  SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS current_schema,
                  SYS_CONTEXT('USERENV','DB_NAME') AS db_name,
                  SYS_CONTEXT('USERENV','SERVER_HOST') AS server_host,
                  SYS_CONTEXT('USERENV','SERVICE_NAME') AS service_name,
                  SYS_CONTEXT('USERENV','IP_ADDRESS') AS ip_address
                FROM DUAL
                """;
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(sql)) {
                if (!rs.next()) {
                    throw new SQLException("session identity query returned no row");
                }
                System.out.println("--- Session ---");
                System.out.println("SESSION_USER: " + rs.getString("session_user"));
                System.out.println("CURRENT_SCHEMA: " + rs.getString("current_schema"));
                System.out.println("DB_NAME: " + rs.getString("db_name"));
                System.out.println("SERVER_HOST: " + rs.getString("server_host"));
                System.out.println("SERVICE_NAME: " + rs.getString("service_name"));
                System.out.println("IP_ADDRESS: " + rs.getString("ip_address"));
                String sessionUser = rs.getString("session_user");
                if (sessionUser == null || !"RAILWAY_READONLY".equalsIgnoreCase(sessionUser)) {
                    throw new SQLException("SESSION_USER is not RAILWAY_READONLY: " + sessionUser);
                }
            }
        }
    }

    private static void dualCheck(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery("SELECT 1 FROM DUAL")) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    throw new SQLException("SELECT 1 FROM DUAL did not return 1");
                }
                System.out.println("DUAL: OK");
            }
        }
    }

    private static void versionCheck(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(
                    "SELECT banner FROM v$version WHERE banner LIKE 'Oracle%' AND ROWNUM <= 3")) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.println("Oracle: " + rs.getString(1));
                }
                if (any) {
                    return;
                }
            }
        } catch (SQLException e) {
            System.out.println("Oracle version via V$VERSION unavailable: " + sanitizeMessage(e.getMessage()));
        }

        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(
                    "SELECT product || ' ' || version FROM product_component_version "
                            + "WHERE product LIKE 'Oracle%' AND ROWNUM <= 3")) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.println("Oracle: " + rs.getString(1));
                }
                if (!any) {
                    System.out.println("Oracle version: UNKNOWN (no accessible version view)");
                }
            }
        } catch (SQLException e) {
            System.out.println("Oracle version: UNKNOWN (" + sanitizeMessage(e.getMessage()) + ")");
        }
    }

    /** Metadata only: whether user DBTC exists. No business table reads. */
    private static void dbtcSchemaPresenceOnly(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM all_users WHERE username = 'DBTC'")) {
                rs.next();
                int users = rs.getInt(1);
                System.out.println("Schema DBTC present in ALL_USERS: " + (users > 0 ? "YES" : "NO"));
            }
        }
    }

    /** @return true if safe to continue; false if write/admin detected */
    private static boolean privilegeCheck(Connection conn) throws SQLException {
        List<String> sessionPrivs = new ArrayList<>();
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(
                    "SELECT privilege FROM session_privs WHERE ROWNUM <= 200 ORDER BY 1")) {
                while (rs.next()) {
                    sessionPrivs.add(rs.getString(1));
                }
            }
        }
        System.out.println("--- Privileges ---");
        System.out.println("SESSION_PRIVS (" + sessionPrivs.size() + "): "
                + (sessionPrivs.isEmpty() ? "<none>" : String.join(", ", sessionPrivs)));

        List<String> roles = new ArrayList<>();
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(
                    "SELECT role FROM session_roles WHERE ROWNUM <= 200 ORDER BY 1")) {
                while (rs.next()) {
                    roles.add(rs.getString(1));
                }
            }
        }
        System.out.println("SESSION_ROLES (" + roles.size() + "): "
                + (roles.isEmpty() ? "<none>" : String.join(", ", roles)));

        // Privilege kinds only — avoid listing business object names from USER_TAB_PRIVS.
        List<String> tabPrivKinds = new ArrayList<>();
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(QUERY_TIMEOUT_SEC);
            try (ResultSet rs = st.executeQuery(
                    "SELECT privilege, COUNT(*) AS cnt FROM user_tab_privs "
                            + "GROUP BY privilege ORDER BY 1")) {
                while (rs.next()) {
                    tabPrivKinds.add(rs.getString(1) + "=" + rs.getInt(2));
                }
            }
        }
        System.out.println("USER_TAB_PRIVS by privilege kind: "
                + (tabPrivKinds.isEmpty() ? "<none>" : String.join(", ", tabPrivKinds)));

        PrivilegeClassifier.Assessment assessment =
                PrivilegeClassifier.assess(sessionPrivs, roles, tabPrivKinds);

        if (!assessment.broadReadNotes().isEmpty()) {
            System.out.println("NOTE: non-write privileges to review (not STOP):");
            for (String note : assessment.broadReadNotes()) {
                System.out.println("  - " + note);
            }
        }

        if (!assessment.isSafeToContinue()) {
            System.out.println("STOP: write/admin markers:");
            for (String d : assessment.writeOrAdmin()) {
                System.out.println("  - " + d);
            }
            return false;
        }
        System.out.println("Privilege check: no write/admin markers in sampled session privileges/roles.");
        return true;
    }

    private static String findForbiddenHost(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        for (String marker : FORBIDDEN_HOST_MARKERS) {
            if (lower.contains(marker.toLowerCase(Locale.ROOT))) {
                return marker;
            }
        }
        return null;
    }

    private static String redactUrl(String url) {
        return url.replaceAll("(?i)^(jdbc:oracle:thin:)[^@/]+@", "$1****@");
    }

    private static String sanitizeMessage(String message) {
        if (message == null) {
            return "<null>";
        }
        return message.replaceAll("(?i)password[=:].*", "password=<redacted>");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void printHelp() {
        System.out.println("""
                Usage:
                  gradlew run --args="--dry-run"
                  gradlew run --args="--connect --allow-potential-prod-smoke-once"

                Environment (never commit):
                  ORACLE_JDBC_URL   jdbc:oracle:thin:@//host:1521/SERVICE
                  ORACLE_USER       RAILWAY_READONLY
                  ORACLE_PASSWORD   (local only)

                Industrial markers (tc-asuvp-03 / RAC.WORLD) stay blocked unless
                --allow-potential-prod-smoke-once is passed for this single run.
                """);
    }

    private OracleSmokeCheck() {
    }
}
