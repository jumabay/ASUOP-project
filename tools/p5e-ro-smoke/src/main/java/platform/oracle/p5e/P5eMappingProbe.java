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
import java.util.Properties;

/**
 * P5-E follow-up: FK dictionary + GETTING vs LOCATION on CONTROL (allow-list only).
 * No SELECT on STATIONS/OPERATIONS/ref tables. Never logs password.
 */
public final class P5eMappingProbe {

    private static final String URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    private static final String CONTROL_CODE = "54787361";
    private static final LocalDate CONTROL_DAY = LocalDate.of(2026, 11, 24);
    private static final int TIMEOUT = 30;

    public static void main(String[] args) throws Exception {
        String password = System.getenv("ASUOP_ORACLE_PASSWORD");
        String user = System.getenv().getOrDefault("ASUOP_ORACLE_USER", "Dbtc");
        if (password == null || password.isBlank()) {
            System.out.println("STOP-ID: ASUOP_ORACLE_PASSWORD unset");
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
            identityGate(conn);

            System.out.println("SECTION=wagon_columns");
            dumpColumns(conn, "WAGON");

            System.out.println("SECTION=srr_columns");
            dumpColumns(conn, "STATEREPORTRECORDS");

            System.out.println("SECTION=fk_wagon");
            dumpFks(conn, "WAGON");

            System.out.println("SECTION=fk_srr");
            dumpFks(conn, "STATEREPORTRECORDS");

            System.out.println("SECTION=ref_columns_stations");
            dumpColumns(conn, "STATIONS");
            System.out.println("SECTION=ref_columns_operations");
            dumpColumns(conn, "OPERATIONS");

            System.out.println("SECTION=control_dates");
            dumpControlDates(conn);
        }
        System.out.println("DONE=MAPPING_PROBE_COMPLETE");
    }

    private static void identityGate(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(TIMEOUT);
            try (ResultSet rs = st.executeQuery(
                    "SELECT USER, SYS_CONTEXT('USERENV','CURRENT_SCHEMA'),"
                            + " SYS_CONTEXT('USERENV','SERVICE_NAME'),"
                            + " SYS_CONTEXT('USERENV','SERVER_HOST') FROM DUAL")) {
                rs.next();
                String u = rs.getString(1);
                String sch = rs.getString(2);
                String svc = rs.getString(3);
                String host = rs.getString(4);
                System.out.println("SECTION=identity");
                System.out.println("session_user=" + u);
                System.out.println("current_schema=" + sch);
                System.out.println("service_name=" + svc);
                System.out.println("server_host=" + host);
                if (!"DBTC".equalsIgnoreCase(u) || !"DBTC".equalsIgnoreCase(sch)
                        || !"RAC.WORLD".equalsIgnoreCase(svc)) {
                    throw new SQLException("STOP-ID identity mismatch");
                }
            }
        }
    }

    private static void dumpColumns(Connection conn, String table) throws SQLException {
        String sql = """
                SELECT column_name, data_type, nullable
                FROM all_tab_columns
                WHERE owner = 'DBTC' AND table_name = ?
                ORDER BY column_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(TIMEOUT);
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("col " + table + "." + rs.getString(1)
                            + ":" + rs.getString(2) + " null=" + rs.getString(3));
                }
            }
        }
    }

    private static void dumpFks(Connection conn, String table) throws SQLException {
        String sql = """
                SELECT c.constraint_name, cc.column_name, cc.position,
                       r.owner AS r_owner, r.table_name AS r_table, rc.column_name AS r_column
                FROM all_constraints c
                JOIN all_cons_columns cc
                  ON cc.owner = c.owner AND cc.constraint_name = c.constraint_name
                LEFT JOIN all_constraints r
                  ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
                LEFT JOIN all_cons_columns rc
                  ON rc.owner = r.owner AND rc.constraint_name = r.constraint_name
                 AND rc.position = cc.position
                WHERE c.owner = 'DBTC'
                  AND c.table_name = ?
                  AND c.constraint_type = 'R'
                ORDER BY c.constraint_name, cc.position
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(TIMEOUT);
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                int n = 0;
                while (rs.next()) {
                    n++;
                    System.out.println("fk " + table + "." + rs.getString(2)
                            + " -> " + rs.getString(4) + "." + rs.getString(5)
                            + "." + rs.getString(6)
                            + " constraint=" + rs.getString(1));
                }
                System.out.println("fk_count_" + table + "=" + n);
            }
        }
    }

    private static void dumpControlDates(Connection conn) throws SQLException {
        long wagonId;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM dbtc.wagon WHERE code = ? FETCH FIRST 1 ROW ONLY")) {
            ps.setQueryTimeout(TIMEOUT);
            ps.setString(1, CONTROL_CODE);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("control_wagon=MISSING");
                    return;
                }
                wagonId = rs.getLong(1);
            }
        }
        LocalDateTime from = CONTROL_DAY.atStartOfDay();
        LocalDateTime to = CONTROL_DAY.plusDays(1).atStartOfDay();
        String sql = """
                SELECT report_id, getting_date, location_date,
                       location_station_id, operation_id, train_code
                FROM dbtc.statereportrecords
                WHERE wagon_id = ?
                  AND getting_date >= ?
                  AND getting_date < ?
                ORDER BY getting_date ASC
                FETCH FIRST 501 ROWS ONLY
                """;
        int n = 0;
        int equal = 0;
        int diff = 0;
        int nullLoc = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(TIMEOUT);
            ps.setLong(1, wagonId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    n++;
                    Timestamp g = rs.getTimestamp(2);
                    Timestamp l = rs.getTimestamp(3);
                    Object stationId = rs.getObject(4);
                    Object opId = rs.getObject(5);
                    String train = rs.getString(6);
                    if (l == null) {
                        nullLoc++;
                    } else if (g != null && g.equals(l)) {
                        equal++;
                    } else {
                        diff++;
                    }
                    if (n <= 5 || n == 13) {
                        System.out.println("row n=" + n
                                + " report_id=" + rs.getObject(1)
                                + " getting_date=" + g
                                + " location_date=" + l
                                + " location_station_id=" + stationId
                                + " operation_id=" + opId
                                + " train_code=" + train
                                + " note=IDs_are_not_business_codes");
                    }
                }
            }
        }
        System.out.println("rows=" + n
                + " getting_eq_location=" + equal
                + " getting_ne_location=" + diff
                + " location_null=" + nullLoc);
        System.out.println("dislocationTime_status=PROVISIONAL_DAY_AXIS_GETTING_DATE_NOT_VERIFIED_AS_FACT");
    }
}
