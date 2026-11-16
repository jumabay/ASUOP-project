package platform.fleet;

/**
 * Data-access port for paged wagon registry (owned by fleet).
 * Implementations may filter/sort/page in memory (synthetic) or in the database (future JDBC).
 */
public interface WagonRegistryPort {

    WagonRegistryPage findPage(WagonRegistryCriteria criteria);
}
