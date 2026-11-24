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
 * Pick up to 3 inventory-park codes that have day tape on 2026-11-24 (bounded).
 * Read-only; no EXECUTE.
 */
public final class G31InventoryParkCardPickProbe {

    private static final String URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    private static final String USER = "Dbtc";
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);

    private G31InventoryParkCardPickProbe() {}

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

        LocalDateTime begin = DAY.atStartOfDay();
        LocalDateTime end = DAY.plusDays(1).atStartOfDay();
        Timestamp beginTs = Timestamp.from(begin.toInstant(ZoneOffset.UTC));
        Timestamp endTs = Timestamp.from(end.toInstant(ZoneOffset.UTC));

        try (Connection c = DriverManager.getConnection(URL, props)) {
            c.setReadOnly(true);
            // CONTROL membership check
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT w.code, w.in_inventory_park, w.deleted, w.wagontype_id
                    FROM dbtc.wagon w WHERE w.code = ?
                    FETCH FIRST 1 ROW ONLY
                    """)) {
                ps.setString(1, "54787361");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("G31_G1 control code=" + rs.getString(1)
                                + " inv=" + rs.getObject(2)
                                + " deleted=" + rs.getObject(3)
                                + " type=" + rs.getObject(4));
                    } else {
                        System.out.println("G31_G1 control=NOT_FOUND");
                    }
                }
            }

            String sql = """
                    SELECT w.code AS wagon_code, COUNT(*) AS line_count
                    FROM dbtc.wagon w
                    JOIN dbtc.statereportrecords r ON r.wagon_id = w.id
                    WHERE w.deleted = 0
                      AND w.in_inventory_park = 1
                      AND w.wagontype_id <> 67
                      AND r.getting_date >= ?
                      AND r.getting_date < ?
                    GROUP BY w.code
                    ORDER BY w.code
                    FETCH FIRST 3 ROWS ONLY
                    """;
            List<String> codes = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setTimestamp(1, beginTs);
                ps.setTimestamp(2, endTs);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String code = rs.getString(1);
                        codes.add(code);
                        System.out.println("G31_G1 pick code=" + code + " day_lines=" + rs.getLong(2));
                    }
                }
            }
            System.out.println("G31_G1 pick_count=" + codes.size());
            System.out.println("G31_G1_PICK_COMPLETE");
        }
    }
}
