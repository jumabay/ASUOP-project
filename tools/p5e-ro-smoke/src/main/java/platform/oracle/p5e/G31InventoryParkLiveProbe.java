package platform.oracle.p5e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Gate 1 live RO: inventory-park page + three card/current/history smokes.
 * Password: env ASUOP_ORACLE_PASSWORD (never printed). No EXECUTE / DML.
 */
public final class G31InventoryParkLiveProbe {

    private static final String URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    private static final String USER = "Dbtc";
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);
    private static final int PAGE_SIZE = 50;
    private static final int DAY_FETCH = 501;

    private G31InventoryParkLiveProbe() {}

    public static void main(String[] args) throws Exception {
        String password = System.getenv("ASUOP_ORACLE_PASSWORD");
        if (password == null || password.isBlank()) {
            System.out.println("STOP-ID: ASUOP_ORACLE_PASSWORD unset");
            System.exit(2);
        }
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", password);
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "15000");
        props.setProperty("oracle.jdbc.ReadTimeout", "30000");

        try (Connection c = DriverManager.getConnection(URL, props)) {
            c.setReadOnly(true);
            System.out.println("G31_G1 identity ok user=" + c.getMetaData().getUserName()
                    + " schema=" + c.getSchema());

            long total = countInventory(c);
            System.out.println("G31_G1 total_elements=" + total);

            List<String> codes = pageCodes(c);
            System.out.println("G31_G1 page_size_returned=" + codes.size());
            if (codes.isEmpty()) {
                System.out.println("STOP-ID: inventory park page empty");
                System.exit(3);
            }
            int n = Math.min(3, codes.size());
            for (int i = 0; i < n; i++) {
                String code = codes.get(i);
                System.out.println("G31_G1 smoke_code[" + i + "]=" + code);
                probeCard(c, code);
                probeDay(c, code);
            }
            System.out.println("G31_G1_PROBE_COMPLETE");
        }
    }

    private static long countInventory(Connection c) throws Exception {
        String sql = """
                SELECT COUNT(*) AS total_elements
                FROM dbtc.wagon w
                WHERE w.deleted = 0
                  AND w.in_inventory_park = 1
                  AND w.wagontype_id <> 67
                """;
        try (PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static List<String> pageCodes(Connection c) throws Exception {
        String sql = """
                SELECT w.code AS wagon_code
                FROM dbtc.wagon w
                LEFT JOIN dbtc.owners o ON o.id = w.owner_id
                LEFT JOIN dbtc.wagontypes wt ON wt.id = w.wagontype_id
                LEFT JOIN dbtc.wagonmodels wm ON wm.id = w.wagon_model_id
                WHERE w.deleted = 0
                  AND w.in_inventory_park = 1
                  AND w.wagontype_id <> 67
                ORDER BY w.code ASC, w.id ASC
                OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<String> codes = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, PAGE_SIZE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    codes.add(rs.getString(1));
                }
            }
        }
        return codes;
    }

    private static void probeCard(Connection c, String code) throws Exception {
        String sql = """
                SELECT w.code AS wagon_code,
                       NVL(m.name, '') AS model_name,
                       NVL(t.name, '') AS type_name,
                       NVL(o.name, '') AS owner_name,
                       NVL(hs.code, '') AS home_station_code,
                       ls.code AS location_station_code,
                       op.idcode AS operation_idcode,
                       w.locationdate AS location_date
                FROM dbtc.wagon w
                LEFT JOIN dbtc.wagonmodels m ON m.id = w.wagon_model_id
                LEFT JOIN dbtc.wagontypes t ON t.id = w.wagontype_id
                LEFT JOIN dbtc.owners o ON o.id = w.owner_id
                LEFT JOIN dbtc.stations hs ON hs.id = w.homestation_id
                LEFT JOIN dbtc.stations ls ON ls.id = w.locationstation_id
                LEFT JOIN dbtc.operations op ON op.id = w.operation_id
                WHERE w.code = ?
                FETCH FIRST 1 ROW ONLY
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("G31_G1 card_missing code=" + code);
                    return;
                }
                System.out.println("G31_G1 card code=" + rs.getString("wagon_code")
                        + " model=" + rs.getString("model_name")
                        + " type=" + rs.getString("type_name")
                        + " station=" + rs.getString("location_station_code")
                        + " op=" + rs.getString("operation_idcode")
                        + " locationdate=" + rs.getTimestamp("location_date"));
            }
        }
    }

    private static void probeDay(Connection c, String code) throws Exception {
        LocalDateTime begin = DAY.atStartOfDay();
        LocalDateTime end = DAY.plusDays(1).atStartOfDay();
        Timestamp beginTs = Timestamp.from(begin.toInstant(ZoneOffset.UTC));
        Timestamp endTs = Timestamp.from(end.toInstant(ZoneOffset.UTC));
        // Bind as TIMESTAMP; Oracle session TZ may differ — same half-open pattern as A1 adapter.
        String sql = """
                SELECT COUNT(*) AS line_count
                FROM (
                  SELECT r.report_id
                  FROM dbtc.statereportrecords r
                  JOIN dbtc.wagon w ON w.id = r.wagon_id
                  WHERE w.code = ?
                    AND r.getting_date >= ?
                    AND r.getting_date < ?
                  FETCH FIRST ? ROWS ONLY
                )
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setTimestamp(2, beginTs);
            ps.setTimestamp(3, endTs);
            ps.setInt(4, DAY_FETCH);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                System.out.println("G31_G1 day code=" + code
                        + " day=" + DAY
                        + " lines_capped=" + rs.getLong(1));
            }
        }
    }
}
