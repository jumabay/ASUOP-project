package platform.fleet;

/**
 * Honesty label for watched-list membership (P5-D).
 * Does not close G31; FIXTURE is never company Active / GetActiveWagonList truth.
 */
public enum WatchedMembershipSource {

    /**
     * Synthetic demo rows from classpath fixture ({@code watched.json}).
     * Not a confirmed operational watch list.
     */
    FIXTURE,

    /**
     * Confirmed RAIL / Oracle membership source is missing while G31 remains OPEN.
     * Page content must stay empty — do not invent Active rules.
     */
    EMPTY_PENDING_G31,

    /**
     * JDBC list port not wired for this profile (e.g. time-boxed dbtc-ro A1/A2 only).
     * Empty page must not be read as confirmed zero wagons.
     */
    SOURCE_NOT_CONNECTED,

    /**
     * DBTC read-only inventory-park subset ({@code WAGON.in_inventory_park = 1}, not type 67).
     * Aligns with researched {@code GetActiveWagonList} predicate shape — not personal watch, not proven old «На слежении» UI.
     */
    DBTC_ACTIVE_INVENTORY_PARK;

    public static final String FIXTURE_NOTE =
            "Демонстрационный synthetic-список (FIXTURE). Это не подтверждённый активный "
                    + "список слежения компании и не эквивалент GetActiveWagonList. G31 OPEN.";

    public static final String EMPTY_PENDING_G31_NOTE =
            "Подтверждённый источник членства слежения отсутствует (G31 OPEN). "
                    + "Список пуст до утверждения источника. Не заполняется угаданными правилами Active.";

    public static final String SOURCE_NOT_CONNECTED_NOTE =
            "Источник не подключён. JDBC для реестра (и watched в профилях без Gate 1) не активен. "
                    + "Пустой список не означает отсутствие вагонов.";

    public static final String DBTC_ACTIVE_INVENTORY_PARK_NOTE =
            "Раздел «Инвентарный парк»: membership DBTC по предикату GetActiveWagonList "
                    + "(deleted=0, in_inventory_park=1, wagontype_id≠67). "
                    + "Не личный watch-list; соответствие старому экрану «На слежении» не заявляется.";

    public String defaultNote() {
        return switch (this) {
            case FIXTURE -> FIXTURE_NOTE;
            case EMPTY_PENDING_G31 -> EMPTY_PENDING_G31_NOTE;
            case SOURCE_NOT_CONNECTED -> SOURCE_NOT_CONNECTED_NOTE;
            case DBTC_ACTIVE_INVENTORY_PARK -> DBTC_ACTIVE_INVENTORY_PARK_NOTE;
        };
    }
}
