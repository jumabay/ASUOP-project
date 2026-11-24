package platform.oracle.jdbc;

import java.util.ArrayList;
import java.util.List;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistrySort;

/**
 * SQL fragments for DBTC inventory-park watched list (GetActiveWagonList-style predicate, no EXECUTE).
 */
final class DbtcInventoryParkWatchedSql {

    static final String MEMBERSHIP_WHERE =
            "w.deleted = 0 AND w.in_inventory_park = 1 AND w.wagontype_id <> 67";

    private DbtcInventoryParkWatchedSql() {
    }

    static String selectSql(WagonRegistryCriteria criteria, BoundWhere bound) {
        String order = criteria.sort() == WagonRegistrySort.CODE_DESC
                ? "w.code DESC, w.id DESC"
                : "w.code ASC, w.id ASC";
        int offset = criteria.page() * criteria.size();
        return """
                SELECT w.code AS wagon_code,
                       NVL(wm.name, '') AS model_name,
                       NVL(wt.name, '') AS type_name,
                       NVL(o.name, '') AS owner_name
                FROM dbtc.wagon w
                LEFT JOIN dbtc.owners o ON o.id = w.owner_id
                LEFT JOIN dbtc.wagontypes wt ON wt.id = w.wagontype_id
                LEFT JOIN dbtc.wagonmodels wm ON wm.id = w.wagon_model_id
                WHERE """
                + " "
                + bound.whereClause()
                + " ORDER BY "
                + order
                + " OFFSET "
                + offset
                + " ROWS FETCH NEXT "
                + criteria.size()
                + " ROWS ONLY";
    }

    static String countSql(BoundWhere bound) {
        return """
                SELECT COUNT(*) AS total_elements
                FROM dbtc.wagon w
                LEFT JOIN dbtc.owners o ON o.id = w.owner_id
                LEFT JOIN dbtc.wagontypes wt ON wt.id = w.wagontype_id
                LEFT JOIN dbtc.wagonmodels wm ON wm.id = w.wagon_model_id
                WHERE """
                + " "
                + bound.whereClause();
    }

    static BoundWhere bindWhere(WagonRegistryCriteria criteria) {
        List<String> parts = new ArrayList<>();
        parts.add(MEMBERSHIP_WHERE);
        List<BoundParam> params = new ArrayList<>();

        if (criteria.search() != null) {
            parts.add("w.code LIKE '%' || :search || '%' ESCAPE '\\'");
            params.add(new BoundParam("search", DbtcLikeEscape.escapeLikeLiteral(criteria.search())));
        }
        if (criteria.owner() != null) {
            parts.add("o.name LIKE '%' || :owner || '%' ESCAPE '\\'");
            params.add(new BoundParam("owner", DbtcLikeEscape.escapeLikeLiteral(criteria.owner())));
        }
        if (criteria.kind() != null) {
            parts.add("wt.name = :kind");
            params.add(new BoundParam("kind", criteria.kind()));
        }

        return new BoundWhere(String.join(" AND ", parts), params);
    }

    record BoundWhere(String whereClause, List<BoundParam> params) {
    }

    record BoundParam(String name, String value) {
    }
}