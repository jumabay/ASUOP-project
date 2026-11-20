package platform.rwl.proto.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/** Safe Oracle session identity (no passwords / JDBC URL secrets). */
public final class ConnectionIdentity {

    private final String user;
    private final String currentSchema;
    private final String dbName;
    private final String serviceName;
    private final String conName;
    private final String banner;

    public ConnectionIdentity(
            String user,
            String currentSchema,
            String dbName,
            String serviceName,
            String conName,
            String banner) {
        this.user = user;
        this.currentSchema = currentSchema;
        this.dbName = dbName;
        this.serviceName = serviceName;
        this.conName = conName;
        this.banner = banner;
    }

    public static ConnectionIdentity probeOracle(Connection connection) throws SQLException {
        String sql =
                """
                SELECT
                  USER AS connected_user,
                  SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS current_schema,
                  SYS_CONTEXT('USERENV', 'DB_NAME') AS db_name,
                  SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service_name,
                  SYS_CONTEXT('USERENV', 'CON_NAME') AS con_name,
                  BANNER
                FROM v$version
                WHERE BANNER LIKE 'Oracle%'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return probeOracleFallback(connection);
            }
            return new ConnectionIdentity(
                    rs.getString("connected_user"),
                    rs.getString("current_schema"),
                    rs.getString("db_name"),
                    rs.getString("service_name"),
                    rs.getString("con_name"),
                    rs.getString("BANNER"));
        } catch (SQLException e) {
            return probeOracleFallback(connection);
        }
    }

    private static ConnectionIdentity probeOracleFallback(Connection connection) throws SQLException {
        String sql =
                """
                SELECT
                  USER AS connected_user,
                  SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS current_schema,
                  SYS_CONTEXT('USERENV', 'DB_NAME') AS db_name,
                  SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service_name,
                  SYS_CONTEXT('USERENV', 'CON_NAME') AS con_name
                FROM dual
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new ConnectionIdentity(
                    rs.getString("connected_user"),
                    rs.getString("current_schema"),
                    rs.getString("db_name"),
                    rs.getString("service_name"),
                    rs.getString("con_name"),
                    null);
        }
    }

    public void print() {
        System.out.println("=== Oracle connection identity ===");
        System.out.println("USER: " + nullToDash(user));
        System.out.println("CURRENT_SCHEMA: " + nullToDash(currentSchema));
        System.out.println("DB_NAME: " + nullToDash(dbName));
        System.out.println("SERVICE_NAME: " + nullToDash(serviceName));
        System.out.println("CON_NAME: " + nullToDash(conName));
        if (banner != null && !banner.isBlank()) {
            System.out.println("BANNER: " + banner);
        }
    }

    /**
     * Target for this prototype is the new RAILDB, not legacy RAC/DBTC archive.
     *
     * @return empty if OK, otherwise refusal reason
     */
    public Optional<String> validateAgainstExpected(
            String expectedService, String expectedDbName, boolean allowArchiveOracle) {
        String service = normalize(serviceName);
        String db = normalize(dbName);

        if (!allowArchiveOracle && (service.contains("RAC.WORLD") || "RAC".equals(db))) {
            return Optional.of(
                    "Refusing legacy archive Oracle (SERVICE_NAME/DB looks like RAC). "
                            + "Target is new RAILDB. Pass --allow-archive-oracle only for explicit archive work.");
        }

        if (expectedService != null && !expectedService.isBlank()) {
            String exp = expectedService.trim().toUpperCase(Locale.ROOT);
            if (!service.equals(exp) && !service.contains(exp)) {
                return Optional.of(
                        "SERVICE_NAME mismatch: got " + nullToDash(serviceName) + ", expected " + expectedService);
            }
        }
        if (expectedDbName != null && !expectedDbName.isBlank()) {
            String exp = expectedDbName.trim().toUpperCase(Locale.ROOT);
            if (!db.equals(exp) && !db.contains(exp)) {
                return Optional.of("DB_NAME mismatch: got " + nullToDash(dbName) + ", expected " + expectedDbName);
            }
        }

        boolean looksLikeRaildb =
                service.contains("RAILDB") || db.contains("RAILDB") || service.contains("RAIL");
        if (expectedService == null
                && expectedDbName == null
                && !allowArchiveOracle
                && !looksLikeRaildb) {
            System.out.println(
                    "NOTE: DB identity does not clearly contain RAILDB. "
                            + "Set EXPECTED_ORACLE_SERVICE / EXPECTED_ORACLE_DB_NAME to pin the target.");
        }
        return Optional.empty();
    }

    public String user() {
        return user;
    }

    public String currentSchema() {
        return currentSchema;
    }

    public String dbName() {
        return dbName;
    }

    public String serviceName() {
        return serviceName;
    }

    public String conName() {
        return conName;
    }

    public String banner() {
        return banner;
    }

    private static String normalize(String v) {
        return v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    }

    private static String nullToDash(String v) {
        return v == null || v.isBlank() ? "-" : v;
    }
}
