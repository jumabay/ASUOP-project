package platform.oracle.runtime;

/**
 * Safe runtime honesty for UI — no JDBC URL, user, password, or host secrets.
 */
public record PlatformRuntimeView(
        String dataSourceKind,
        String displayLabel,
        boolean registryConnected,
        boolean watchedConnected
) {
    public static final String KIND_SYNTHETIC = "SYNTHETIC";
    public static final String KIND_DBTC_RO = "DBTC_RO";
    public static final String KIND_RAILDB_TEST = "RAILDB_TEST";
}
