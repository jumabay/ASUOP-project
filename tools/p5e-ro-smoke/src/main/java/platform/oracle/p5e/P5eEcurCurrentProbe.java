package platform.oracle.p5e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/**
 * One-shot P5-E-CUR CONTROL probe: WAGON master current + TRAIN column metadata.
 * Password from env ASUOP_ORACLE_PASSWORD only. Does not print secrets.
 */
public final class P5eEcurCurrentProbe {

    private static final String URL = "jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD";
    private static final String USER = "Dbtc";
    private static final String CONTROL = "54787361";

    private P5eEcurCurrentProbe() {}

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
            System.out.println("E_CUR identity ok schema=" + c.getSchema());

            String sql = """
                    SELECT w.code AS wagon_code,
                           ls.code AS location_station_code,
                           op.idcode AS operation_idcode,
                           op.code AS operation_code_raw,
                           w.locationdate AS location_date,
                           w.reportdate AS report_date
                    FROM dbtc.wagon w
                    LEFT JOIN dbtc.stations ls ON ls.id = w.locationstation_id
                    LEFT JOIN dbtc.operations op ON op.id = w.operation_id
                    WHERE w.code = ?
                    FETCH FIRST 1 ROW ONLY
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, CONTROL);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("E_CUR wagon_not_found code=" + CONTROL);
                    } else {
                        System.out.println("E_CUR wagon_code=" + rs.getString("wagon_code"));
                        System.out.println("E_CUR station_code=" + rs.getString("location_station_code"));
                        System.out.println("E_CUR operation_idcode=" + rs.getString("operation_idcode"));
                        System.out.println("E_CUR operation_code_raw=" + rs.getString("operation_code_raw"));
                        System.out.println("E_CUR location_date=" + rs.getTimestamp("location_date"));
                        System.out.println("E_CUR report_date_diagnostic=" + rs.getTimestamp("report_date"));
                    }
                }
            }

            String meta = """
                    SELECT column_name, data_type
                    FROM all_tab_columns
                    WHERE owner = 'DBTC' AND table_name = 'WAGON' AND column_name LIKE '%TRAIN%'
                    ORDER BY column_id
                    """;
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(meta)) {
                int n = 0;
                while (rs.next()) {
                    n++;
                    System.out.println("E_CUR train_col=" + rs.getString(1) + " type=" + rs.getString(2));
                }
                if (n == 0) {
                    System.out.println("E_CUR train_col=NONE");
                }
                System.out.println("E_CUR train_col_count=" + n + " (discovery≠mapping)");
            }
        }
        System.out.println("E_CUR_PROBE_COMPLETE");
    }
}
