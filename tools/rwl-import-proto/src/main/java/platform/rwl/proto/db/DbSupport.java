package platform.rwl.proto.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import platform.rwl.proto.config.RwlConfig;

public final class DbSupport {

    public enum Dialect {
        ORACLE,
        H2
    }

    private DbSupport() {
    }

    public static Connection openOracle(RwlConfig config) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", config.oracleUser());
        props.setProperty("password", config.oraclePassword());
        props.setProperty("oracle.jdbc.timezoneAsRegion", "false");
        // Bind/read timestamps in UTC regardless of JVM default zone
        props.setProperty("oracle.jdbc.defaultNChar", "false");
        Connection c = DriverManager.getConnection(config.oracleJdbcUrl(), props);
        c.setAutoCommit(false);
        return c;
    }

    public static Connection openH2(String jdbcUrl) throws SQLException {
        Connection c = DriverManager.getConnection(jdbcUrl, "sa", "");
        c.setAutoCommit(false);
        return c;
    }

    public static void applyDdl(Connection connection, Dialect dialect) throws SQLException, IOException {
        String resource = dialect == Dialect.ORACLE ? "/ddl/oracle-staging.sql" : "/ddl/h2-staging.sql";
        String ddl = readResource(resource);
        for (String statement : splitSql(ddl)) {
            try (Statement st = connection.createStatement()) {
                st.execute(statement);
            } catch (SQLException e) {
                // idempotent-ish create for repeated local runs
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
                if (msg.contains("already exists") || msg.contains("ora-00955") || msg.contains("name is already used")) {
                    continue;
                }
                throw e;
            }
        }
        connection.commit();
    }

    public static boolean stagingTablesExist(Connection connection) throws SQLException {
        try (var st = connection.createStatement();
                var rs = st.executeQuery(
                        "SELECT COUNT(*) FROM rwl_import_run WHERE 1=0")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = DbSupport.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> splitSql(String ddl) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new java.io.ByteArrayInputStream(ddl.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    String sql = current.toString().trim();
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }
                    if (!sql.isEmpty()) {
                        statements.add(sql);
                    }
                    current.setLength(0);
                }
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail.endsWith(";") ? tail.substring(0, tail.length() - 1) : tail);
        }
        return statements;
    }
}
