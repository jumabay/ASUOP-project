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
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Approved ref SELECT delta: metadata + PK lookups for CONTROL only.
 * Preserves leading zeros in VARCHAR2 codes. No full dictionary scan.
 */
public final class P5eRefLookupProbe {

    private static final String URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    private static final String CONTROL = "54787361";
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);
    private static final int T = 30;

    public static void main(String[] args) throws Exception {
        String password = System.getenv("ASUOP_ORACLE_PASSWORD");
        String user = System.getenv().getOrDefault("ASUOP_ORACLE_USER", "Dbtc");
        if (password == null || password.isBlank()) {
            System.out.println("STOP-ID: password unset");
            System.exit(3);
            return;
        }
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "15000");
        props.setProperty("oracle.jdbc.ReadTimeout", "30000");

        try (Connection c = DriverManager.getConnection(URL, props)) {
            c.setReadOnly(true);
            c.setAutoCommit(false);
            gate(c);
            meta(c, "WAGONMODELS");
            meta(c, "WAGONTYPES");
            meta(c, "OWNERS");

            long wagonId;
            Long modelId;
            Long typeId;
            Long ownerId;
            Long homeId;
            Number inventory;
            try (PreparedStatement ps = c.prepareStatement(
                    """
                    SELECT id, wagon_model_id, wagontype_id, owner_id, homestation_id, in_inventory_park
                    FROM dbtc.wagon WHERE code = ? FETCH FIRST 1 ROW ONLY
                    """)) {
                ps.setQueryTimeout(T);
                ps.setString(1, CONTROL);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("STOP: wagon missing");
                        System.exit(4);
                        return;
                    }
                    wagonId = rs.getLong(1);
                    modelId = toLong(rs.getObject(2));
                    typeId = toLong(rs.getObject(3));
                    ownerId = toLong(rs.getObject(4));
                    homeId = toLong(rs.getObject(5));
                    inventory = (Number) rs.getObject(6);
                }
            }
            System.out.println("SECTION=wagon_raw");
            System.out.println("wagon_id=" + wagonId);
            System.out.println("wagon_model_id=" + modelId);
            System.out.println("wagontype_id=" + typeId);
            System.out.println("owner_id=" + ownerId);
            System.out.println("homestation_id=" + homeId);
            System.out.println("in_inventory_park_raw=" + inventory
                    + " note=single_CONTROL_sample_not_global_proof");

            lookup(c, "WAGONMODELS", modelId);
            lookup(c, "WAGONTYPES", typeId);
            lookup(c, "OWNERS", ownerId);
            lookup(c, "STATIONS", homeId);

            Set<Long> stationIds = new LinkedHashSet<>();
            Set<Long> opIds = new LinkedHashSet<>();
            LocalDateTime from = DAY.atStartOfDay();
            LocalDateTime to = DAY.plusDays(1).atStartOfDay();
            try (PreparedStatement ps = c.prepareStatement(
                    """
                    SELECT location_station_id, operation_id
                    FROM dbtc.statereportrecords
                    WHERE wagon_id = ? AND getting_date >= ? AND getting_date < ?
                    FETCH FIRST 501 ROWS ONLY
                    """)) {
                ps.setQueryTimeout(T);
                ps.setLong(1, wagonId);
                ps.setTimestamp(2, Timestamp.valueOf(from));
                ps.setTimestamp(3, Timestamp.valueOf(to));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Object s = rs.getObject(1);
                        Object o = rs.getObject(2);
                        if (s != null) {
                            stationIds.add(((Number) s).longValue());
                        }
                        if (o != null) {
                            opIds.add(((Number) o).longValue());
                        }
                    }
                }
            }
            System.out.println("SECTION=day_fk_ids");
            System.out.println("distinct_location_station_ids=" + stationIds);
            System.out.println("distinct_operation_ids=" + opIds);
            for (Long id : stationIds) {
                lookup(c, "STATIONS", id);
            }
            for (Long id : opIds) {
                lookup(c, "OPERATIONS", id);
            }
        }
        System.out.println("DONE=REF_LOOKUP_COMPLETE");
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        return ((Number) o).longValue();
    }

    private static void gate(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.setQueryTimeout(T);
            try (ResultSet rs = st.executeQuery(
                    "SELECT USER, SYS_CONTEXT('USERENV','CURRENT_SCHEMA'),"
                            + " SYS_CONTEXT('USERENV','SERVICE_NAME') FROM DUAL")) {
                rs.next();
                System.out.println("SECTION=identity");
                System.out.println("session_user=" + rs.getString(1));
                System.out.println("current_schema=" + rs.getString(2));
                System.out.println("service_name=" + rs.getString(3));
                if (!"DBTC".equalsIgnoreCase(rs.getString(1))
                        || !"RAC.WORLD".equalsIgnoreCase(rs.getString(3))) {
                    throw new SQLException("STOP-ID");
                }
            }
        }
    }

    private static void meta(Connection c, String table) throws SQLException {
        System.out.println("SECTION=meta_" + table);
        try (PreparedStatement ps = c.prepareStatement(
                """
                SELECT column_name, data_type FROM all_tab_columns
                WHERE owner='DBTC' AND table_name=? ORDER BY column_id
                """)) {
            ps.setQueryTimeout(T);
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("col " + table + "." + rs.getString(1) + ":" + rs.getString(2));
                }
            }
        }
    }

    private static void lookup(Connection c, String table, Long id) throws SQLException {
        if (id == null) {
            System.out.println("lookup " + table + " id=null SKIP");
            return;
        }
        String sql = switch (table) {
            case "STATIONS" -> "SELECT id, code, name FROM dbtc.stations WHERE id=? FETCH FIRST 1 ROW ONLY";
            case "OPERATIONS" -> "SELECT id, code, name, idcode FROM dbtc.operations WHERE id=? FETCH FIRST 1 ROW ONLY";
            case "WAGONMODELS" -> "SELECT * FROM dbtc.wagonmodels WHERE id=? FETCH FIRST 1 ROW ONLY";
            case "WAGONTYPES" -> "SELECT * FROM dbtc.wagontypes WHERE id=? FETCH FIRST 1 ROW ONLY";
            case "OWNERS" -> "SELECT * FROM dbtc.owners WHERE id=? FETCH FIRST 1 ROW ONLY";
            default -> throw new IllegalArgumentException(table);
        };
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setQueryTimeout(T);
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("lookup " + table + " id=" + id + " NOT_FOUND");
                    return;
                }
                var md = rs.getMetaData();
                StringBuilder sb = new StringBuilder("lookup " + table + " id=" + id);
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    String col = md.getColumnName(i);
                    Object val = rs.getObject(i);
                    if (val instanceof String s) {
                        sb.append(' ').append(col).append("=[").append(s).append(']');
                    } else {
                        sb.append(' ').append(col).append('=').append(val);
                    }
                }
                System.out.println(sb);
            }
        }
    }
}
