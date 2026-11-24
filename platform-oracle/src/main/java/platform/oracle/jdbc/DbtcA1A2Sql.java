package platform.oracle.jdbc;

/**
 * Bound SQL drafts for P5-E A1/A2 on DBTC.
 * Ref joins (stations/operations/models/types/owners) are APPROVED time-boxed
 * under profile {@code dbtc-ro}; live adapters are in the same package.
 */
public final class DbtcA1A2Sql {

    private DbtcA1A2Sql() {}

    /** Resolve wagon id + passport raw columns (no ref joins). */
    public static final String WAGON_PASSPORT_CORE = """
            SELECT w.id AS wagon_id,
                   w.code AS wagon_code,
                   w.wagon_model_id AS wagon_model_id,
                   w.wagontype_id AS wagon_type_id,
                   w.owner_id AS owner_id,
                   w.homestation_id AS home_station_id,
                   w.in_inventory_park AS in_inventory_park
            FROM dbtc.wagon w
            WHERE w.code = :wagon_code
            FETCH FIRST 1 ROW ONLY
            """;

    /**
     * Passport with home station codes (approved ref SELECT delta).
     * Full passport with models/types/owners: see {@link DbtcJdbcWagonCardAdapter}.
     */
    public static final String WAGON_PASSPORT_WITH_HOME_STATION = """
            SELECT w.id AS wagon_id,
                   w.code AS wagon_code,
                   w.wagon_model_id AS wagon_model_id,
                   w.wagontype_id AS wagon_type_id,
                   w.owner_id AS owner_id,
                   w.homestation_id AS home_station_id,
                   NVL(hs.code, '') AS home_station_code,
                   NVL(hs.name, '') AS home_station_name,
                   w.in_inventory_park AS in_inventory_park
            FROM dbtc.wagon w
            LEFT JOIN dbtc.stations hs ON hs.id = w.homestation_id
            WHERE w.code = :wagon_code
            FETCH FIRST 1 ROW ONLY
            """;

    /**
     * Day lines on GETTING_DATE axis (PROVISIONAL dislocationTime — not verified as fact).
     * Station/operation codes empty without joins.
     */
    public static final String STATE_REPORT_DAY_CORE = """
            SELECT r.report_id AS report_id,
                   :wagon_code AS wagon_code,
                   r.getting_date AS getting_date,
                   r.location_date AS location_date,
                   r.location_station_id AS location_station_id,
                   r.operation_id AS operation_id
            FROM dbtc.statereportrecords r
            WHERE r.wagon_id = :wagon_id
              AND r.getting_date >= :day_from
              AND r.getting_date < :day_to
            ORDER BY r.getting_date ASC
            FETCH FIRST :fetch_limit ROWS ONLY
            """;

    /**
     * Day lines with business codes. REQUIRES SELECT on STATIONS + OPERATIONS.
     * Still maps dislocationTime from GETTING_DATE (provisional / DT1 OPEN).
     */
    public static final String STATE_REPORT_DAY_WITH_CODES = """
            SELECT r.report_id AS report_id,
                   :wagon_code AS wagon_code,
                   r.getting_date AS getting_date,
                   r.location_date AS location_date,
                   NVL(st.code, '') AS station_code,
                   NVL(op.code, '') AS operation_code
            FROM dbtc.statereportrecords r
            LEFT JOIN dbtc.stations st ON st.id = r.location_station_id
            LEFT JOIN dbtc.operations op ON op.id = r.operation_id
            WHERE r.wagon_id = :wagon_id
              AND r.getting_date >= :day_from
              AND r.getting_date < :day_to
            ORDER BY r.getting_date ASC
            FETCH FIRST :fetch_limit ROWS ONLY
            """;
}
