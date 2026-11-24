package platform.oracle.jdbc;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import platform.fleet.WagonCard;
import platform.fleet.WagonCardPort;

/**
 * A2 passport from DBTC with ref joins (approved P5-E delta). currentState always null.
 */
@Component
@Profile("dbtc-ro")
public class DbtcJdbcWagonCardAdapter implements WagonCardPort {

    private static final String SQL = """
            SELECT w.code AS wagon_code,
                   NVL(m.name, '') AS model_name,
                   NVL(t.name, '') AS type_name,
                   NVL(o.name, '') AS owner_name,
                   NVL(hs.code, '') AS home_station_code,
                   NVL(hs.name, '') AS home_station_name,
                   w.in_inventory_park AS in_inventory_park
            FROM dbtc.wagon w
            LEFT JOIN dbtc.wagonmodels m ON m.id = w.wagon_model_id
            LEFT JOIN dbtc.wagontypes t ON t.id = w.wagontype_id
            LEFT JOIN dbtc.owners o ON o.id = w.owner_id
            LEFT JOIN dbtc.stations hs ON hs.id = w.homestation_id
            WHERE w.code = :wagon_code
            FETCH FIRST 1 ROW ONLY
            """;

    private final JdbcClient jdbc;

    public DbtcJdbcWagonCardAdapter(JdbcClient dbtcRoJdbcClient) {
        this.jdbc = dbtcRoJdbcClient;
    }

    @Override
    public Optional<WagonCard> findByCode(String wagonCode) {
        return jdbc.sql(SQL)
                .param("wagon_code", wagonCode)
                .query((rs, rowNum) -> DbtcWagonCardFactory.fromCoreRow(
                        rs.getString("wagon_code"),
                        rs.getString("home_station_code"),
                        rs.getString("home_station_name"),
                        (Number) rs.getObject("in_inventory_park"),
                        rs.getString("model_name"),
                        rs.getString("type_name"),
                        rs.getString("owner_name")))
                .optional();
    }
}
