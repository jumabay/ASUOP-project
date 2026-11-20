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
    EMPTY_PENDING_G31;

    public static final String FIXTURE_NOTE =
            "Демонстрационный synthetic-список (FIXTURE). Это не подтверждённый активный "
                    + "список слежения компании и не эквивалент GetActiveWagonList. G31 OPEN.";

    public static final String EMPTY_PENDING_G31_NOTE =
            "Подтверждённый источник членства слежения отсутствует (G31 OPEN). "
                    + "Список пуст до утверждения источника. Не заполняется угаданными правилами Active.";

    public String defaultNote() {
        return switch (this) {
            case FIXTURE -> FIXTURE_NOTE;
            case EMPTY_PENDING_G31 -> EMPTY_PENDING_G31_NOTE;
        };
    }
}
