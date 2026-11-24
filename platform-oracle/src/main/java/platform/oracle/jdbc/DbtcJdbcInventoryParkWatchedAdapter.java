package platform.oracle.jdbc;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WatchedMembershipSource;
import platform.fleet.WatchedWagonItem;
import platform.fleet.WatchedWagonPage;
import platform.fleet.WatchedWagonPort;

/**
 * P5-G31 Gate 1: inventory-park membership from {@code DBTC.WAGON} predicate (not personal watch list).
 */
@Component
@Profile("dbtc-ro")
public class DbtcJdbcInventoryParkWatchedAdapter implements WatchedWagonPort {

    private final JdbcClient jdbc;

    public DbtcJdbcInventoryParkWatchedAdapter(JdbcClient dbtcRoJdbcClient) {
        this.jdbc = dbtcRoJdbcClient;
    }

    @Override
    public WatchedWagonPage findPage(WagonRegistryCriteria criteria) {
        DbtcInventoryParkWatchedSql.BoundWhere bound = DbtcInventoryParkWatchedSql.bindWhere(criteria);
        long total = queryTotal(bound);
        List<WatchedWagonItem> content = queryPage(criteria, bound);
        return WatchedWagonPage.of(
                content,
                criteria.page(),
                criteria.size(),
                total,
                WatchedWagonPage.computeTotalPages(total, criteria.size()),
                WatchedMembershipSource.DBTC_ACTIVE_INVENTORY_PARK);
    }

    private long queryTotal(DbtcInventoryParkWatchedSql.BoundWhere bound) {
        JdbcClient.StatementSpec spec = jdbc.sql(DbtcInventoryParkWatchedSql.countSql(bound));
        spec = bindAll(spec, bound);
        return spec.query((rs, rowNum) -> rs.getLong(1)).single();
    }

    private List<WatchedWagonItem> queryPage(
            WagonRegistryCriteria criteria, DbtcInventoryParkWatchedSql.BoundWhere bound) {
        JdbcClient.StatementSpec spec = jdbc.sql(DbtcInventoryParkWatchedSql.selectSql(criteria, bound));
        spec = bindAll(spec, bound);
        return spec.query((rs, rowNum) -> new WatchedWagonItem(
                        requireNonBlank(rs.getString("wagon_code"), "wagon_code"),
                        nullToEmpty(rs.getString("model_name")),
                        nullToEmpty(rs.getString("type_name")),
                        nullToEmpty(rs.getString("owner_name")),
                        "",
                        "",
                        true))
                .list();
    }

    /** Oracle treats '' as NULL; WatchedWagonItem forbids null passport strings. */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("inventory park row missing " + field);
        }
        return value;
    }

    private static JdbcClient.StatementSpec bindAll(
            JdbcClient.StatementSpec spec, DbtcInventoryParkWatchedSql.BoundWhere bound) {
        for (DbtcInventoryParkWatchedSql.BoundParam param : bound.params()) {
            spec = spec.param(param.name(), param.value());
        }
        return spec;
    }
}