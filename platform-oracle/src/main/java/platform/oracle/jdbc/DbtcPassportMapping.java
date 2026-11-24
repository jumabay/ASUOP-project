package platform.oracle.jdbc;

/**
 * Confirmed DBTC passport graph from P5-E ALL_CONSTRAINTS probe (2026-11-24).
 * Display columns for models/types/owners remain UNKNOWN until ref metadata/SELECT approved.
 */
public final class DbtcPassportMapping {

    private DbtcPassportMapping() {}

    public static final String WAGON_CODE = "CODE";
    public static final String WAGON_MODEL_ID = "WAGON_MODEL_ID";
    public static final String WAGON_TYPE_ID = "WAGONTYPE_ID";
    public static final String OWNER_ID = "OWNER_ID";
    public static final String HOME_STATION_ID = "HOMESTATION_ID";
    public static final String IN_INVENTORY_PARK = "IN_INVENTORY_PARK";

    public static final String FK_MODEL = "WAGON.WAGON_MODEL_ID -> DBTC.WAGONMODELS.ID";
    public static final String FK_TYPE = "WAGON.WAGONTYPE_ID -> DBTC.WAGONTYPES.ID";
    public static final String FK_OWNER = "WAGON.OWNER_ID -> DBTC.OWNERS.ID";
    public static final String FK_HOME = "WAGON.HOMESTATION_ID -> DBTC.STATIONS.ID";

    public static final String STATIONS_CODE = "STATIONS.CODE";
    public static final String STATIONS_NAME = "STATIONS.NAME";
    public static final String OPERATIONS_CODE = "OPERATIONS.CODE";

    public static final String SRR_LOCATION_STATION_FK =
            "STATEREPORTRECORDS.LOCATION_STATION_ID -> DBTC.STATIONS.ID";
    public static final String SRR_OPERATION_FK =
            "STATEREPORTRECORDS.OPERATION_ID -> DBTC.OPERATIONS.ID";
}
