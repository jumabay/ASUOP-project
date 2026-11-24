package platform.oracle.p5e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

/**
 * Gate 1 scenario smoke: inventory total + CODE search + 3 card/current/day drills.
 * Password env ASUOP_ORACLE_PASSWORD only. No EXECUTE/DML.
 */
public final class G31InventoryParkScenarioProbe {

    private static final String URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    private static final String USER = "Dbtc";
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);
    private static final String[] DRILL = {"28024669", "28025377", "28053023"};
    private static final String CONTROL = "54787361";

    private G31InventoryParkScenarioProbe() {}

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

        Timestamp beginTs = Timestamp.from(DAY.atStartOfDay().toInstant(ZoneOffset.UTC));
        Timestamp endTs = Timestamp.from(DAY.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        try (Connection c = DriverManager.getConnection(URL, props)) {
            c.setReadOnly(true);
            System.out.println("G31_G1S identity ok user=" + c.getMetaData().getUserName()
                    + " schema=" + c.getSchema());

            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT COUNT(*) FROM dbtc.wagon w
                    WHERE w.deleted = 0 AND w.in_inventory_park = 1 AND w.wagontype_id <> 67
                    """);
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                System.out.println("G31_G1S total_elements=" + rs.getLong(1));
            }

            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT COUNT(*) FROM dbtc.wagon w
                    WHERE w.deleted = 0 AND w.in_inventory_park = 1 AND w.wagontype_id <> 67
                      AND w.code LIKE '%' || ? || '%' ESCAPE '\\'
                    """)) {
                ps.setString(1, CONTROL);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    System.out.println("G31_G1S search_control_hits=" + rs.getLong(1));
                }
            }

            for (String code : DRILL) {
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT w.code,
                               NVL(m.name,'') AS model_name,
                               NVL(t.name,'') AS type_name,
                               NVL(o.name,'') AS owner_name,
                               ls.code AS station_code,
                               NVL(op.idcode, op.code) AS operation_code,
                               w.locationdate
                        FROM dbtc.wagon w
                        LEFT JOIN dbtc.wagonmodels m ON m.id = w.wagon_model_id
                        LEFT JOIN dbtc.wagontypes t ON t.id = w.wagontype_id
                        LEFT JOIN dbtc.owners o ON o.id = w.owner_id
                        LEFT JOIN dbtc.stations ls ON ls.id = w.locationstation_id
                        LEFT JOIN dbtc.operations op ON op.id = w.operation_id
                        WHERE w.code = ?
                          AND w.deleted = 0 AND w.in_inventory_park = 1 AND w.wagontype_id <> 67
                        FETCH FIRST 1 ROW ONLY
                        """)) {
                    ps.setString(1, code);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("G31_G1S MISSING_IN_PARK code=" + code);
                            continue;
                        }
                        System.out.println("G31_G1S card code=" + rs.getString(1)
                                + " model=" + rs.getString(2)
                                + " type=" + rs.getString(3)
                                + " owner=" + rs.getString(4)
                                + " station=" + rs.getString(5)
                                + " op=" + rs.getString(6)
                                + " locationdate=" + rs.getTimestamp(7));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT COUNT(*) FROM (
                          SELECT r.report_id
                          FROM dbtc.statereportrecords r
                          JOIN dbtc.wagon w ON w.id = r.wagon_id
                          LEFT JOIN dbtc.stations s ON s.id = r.location_station_id
                          LEFT JOIN dbtc.operations op ON op.id = r.operation_id
                          WHERE w.code = ?
                            AND r.getting_date >= ? AND r.getting_date < ?
                          FETCH FIRST 501 ROWS ONLY
                        )
                        """)) {
                    ps.setString(1, code);
                    ps.setTimestamp(2, beginTs);
                    ps.setTimestamp(3, endTs);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        System.out.println("G31_G1S day code=" + code + " lines=" + rs.getLong(1));
                    }
                }
            }
            System.out.println("G31_G1S_COMPLETE");
        }
    }
}
