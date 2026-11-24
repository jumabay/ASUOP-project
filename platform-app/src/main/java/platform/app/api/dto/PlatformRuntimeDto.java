package platform.app.api.dto;

/**
 * Safe client-facing runtime honesty. Never includes JDBC URL, credentials, or hosts.
 */
public record PlatformRuntimeDto(
        String dataSourceKind,
        String displayLabel,
        boolean registryConnected,
        boolean watchedConnected
) {
}
