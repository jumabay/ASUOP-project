package platform.oracle.jdbc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import platform.fleet.WagonRegistryCriteria;
import platform.fleet.WagonRegistrySort;

class DbtcInventoryParkWatchedSqlTest {

    @Test
    void membershipPredicateUsesNullSafeNotEqual() {
        DbtcInventoryParkWatchedSql.BoundWhere bound = DbtcInventoryParkWatchedSql.bindWhere(
                criteria(null, null, null));
        assertTrue(bound.whereClause().contains(DbtcInventoryParkWatchedSql.MEMBERSHIP_WHERE));
        assertTrue(bound.whereClause().contains("wagontype_id <> 67"));
        assertFalse(bound.whereClause().toUpperCase().contains("NVL"));
    }

    @Test
    void countAndSelectKeepSpaceAfterWhere() {
        DbtcInventoryParkWatchedSql.BoundWhere bound = DbtcInventoryParkWatchedSql.bindWhere(
                criteria(null, null, null));
        String count = DbtcInventoryParkWatchedSql.countSql(bound);
        String select = DbtcInventoryParkWatchedSql.selectSql(criteria(null, null, null), bound);
        assertTrue(count.contains("WHERE w.deleted") || count.contains("WHERE  w.deleted"));
        assertFalse(count.contains("WHEREw."));
        assertFalse(select.contains("WHEREw."));
    }

    @Test
    void searchAddsEscapedLikeOnCode() {
        DbtcInventoryParkWatchedSql.BoundWhere bound = DbtcInventoryParkWatchedSql.bindWhere(
                criteria("100%", null, null));
        assertTrue(bound.whereClause().contains("w.code LIKE '%' || :search || '%' ESCAPE '\\'"));
        assertEqualsParam(bound, "search", "100\\%");
    }

    @Test
    void selectUsesAllowedTablesOnly() {
        String sql = DbtcInventoryParkWatchedSql.selectSql(criteria(null, null, null), DbtcInventoryParkWatchedSql
                .bindWhere(criteria(null, null, null)));
        assertFalse(sql.toUpperCase().contains("STATIONS"));
        assertTrue(sql.contains("dbtc.wagon w"));
        assertTrue(sql.contains("dbtc.owners"));
        assertTrue(sql.contains("dbtc.wagontypes"));
        assertTrue(sql.contains("dbtc.wagonmodels"));
    }

    @Test
    void sortAscAndDesc() {
        var asc = DbtcInventoryParkWatchedSql.selectSql(
                new WagonRegistryCriteria(null, null, null, 0, 50, WagonRegistrySort.CODE_ASC),
                DbtcInventoryParkWatchedSql.bindWhere(criteria(null, null, null)));
        assertTrue(asc.contains("ORDER BY w.code ASC, w.id ASC"));

        var desc = DbtcInventoryParkWatchedSql.selectSql(
                new WagonRegistryCriteria(null, null, null, 0, 50, WagonRegistrySort.CODE_DESC),
                DbtcInventoryParkWatchedSql.bindWhere(criteria(null, null, null)));
        assertTrue(desc.contains("ORDER BY w.code DESC, w.id DESC"));
    }

  @Test
  void countSharesWhereWithSelect() {
    DbtcInventoryParkWatchedSql.BoundWhere bound = DbtcInventoryParkWatchedSql.bindWhere(
        criteria("55", null, null));
    String count = DbtcInventoryParkWatchedSql.countSql(bound);
    String select = DbtcInventoryParkWatchedSql.selectSql(criteria("55", null, null), bound);
    assertTrue(count.contains(bound.whereClause()));
    assertTrue(select.contains(bound.whereClause()));
  }

    private static WagonRegistryCriteria criteria(String search, String owner, String kind) {
        return new WagonRegistryCriteria(search, owner, kind, 0, 50, WagonRegistrySort.CODE_ASC);
    }

    private static void assertEqualsParam(
            DbtcInventoryParkWatchedSql.BoundWhere bound, String name, String value) {
        boolean found = bound.params().stream()
                .anyMatch(p -> p.name().equals(name) && p.value().equals(value));
        assertTrue(found, "expected param " + name + "=" + value);
    }
}