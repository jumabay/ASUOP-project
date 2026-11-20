/**
 * P4-0 fail-fast stubs for read ports under profile {@code raildb-test}.
 *
 * <p>Beans implement {@code WagonCardPort}, {@code WagonRegistryPort},
 * {@code WatchedWagonPort}, and {@code StateReportDayPort} and throw
 * {@link UnsupportedOperationException} with a shared DENIED diagnostic.
 * Still no DataSource, JDBC URL, credentials, or network I/O.
 * Activate JDBC to standalone {@code RAILDB} (schema {@code RAIL_APP}) only after
 * APPROVALS allow it. Do not use the legacy {@code RAIL_APP} schema inside RAC
 * as the production store (ADR-0009).
 */
package platform.oracle.raildb;
